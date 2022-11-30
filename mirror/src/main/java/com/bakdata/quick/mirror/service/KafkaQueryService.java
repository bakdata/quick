/*
 *    Copyright 2022 bakdata GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.bakdata.quick.mirror.service;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.mirror.DefaultMirrorClient;
import com.bakdata.quick.common.api.client.mirror.DefaultMirrorRequestManager;
import com.bakdata.quick.common.api.client.mirror.HeaderConstants;
import com.bakdata.quick.common.api.client.mirror.MirrorHost;
import com.bakdata.quick.common.api.client.mirror.MirrorValueParser;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.context.MirrorContextProvider;
import com.bakdata.quick.mirror.context.RangeIndexProperties;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.indexer.ReadRangeIndexer;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.exceptions.HttpStatusException;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

/**
 * Service for querying a kafka state store.
 *
 * @param <K> type of the state store's key
 * @param <V> type of the state store's value
 */
@Slf4j
public class KafkaQueryService<K, V> implements QueryService<V> {
    private final MirrorContext<K, V> queryContext;
    private final HttpClient client;
    private final KafkaStreams streams;
    private final HostInfo hostInfo;
    private final Serializer<K> keySerializer;
    private final TypeResolver<K> keyResolver;
    private final TypeResolver<V> valueResolver;
    private final StoreQueryParameters<ReadOnlyKeyValueStore<K, V>> pointStoreQueryParameters;
    private final RangeIndexProperties rangeIndexProperties;
    private final SchemaExtractor schemaExtractor;
    @Nullable
    private StoreQueryParameters<ReadOnlyKeyValueStore<String, V>> rangeStoreQueryParameters;
    @Nullable
    private ReadRangeIndexer<K, ?> rangeIndexer = null;

    /**
     * Injectable constructor.
     */
    @Inject
    public KafkaQueryService(final HttpClient client, final SchemaExtractor schemaExtractor,
        final MirrorContextProvider<K, V> contextProvider) {
        this.client = client;
        this.schemaExtractor = schemaExtractor;

        this.queryContext = contextProvider.get();

        this.streams = this.queryContext.getStreams();
        this.hostInfo = this.queryContext.getHostInfo();
        this.keySerializer = this.queryContext.getKeySerde().serializer();
        this.keyResolver = this.queryContext.getIndexInputStream().getKeyData().getResolver();
        this.valueResolver = this.queryContext.getIndexInputStream().getValueData().getResolver();
        this.rangeIndexProperties = this.queryContext.getRangeIndexProperties();

        log.debug("Initializing KafkaQueryService for point index");
        this.pointStoreQueryParameters =
            StoreQueryParameters.fromNameAndType(this.queryContext.getPointStoreName(),
                QueryableStoreTypes.keyValueStore());

        if (this.rangeIndexProperties.isEnabled()) {
            this.initializeQueryServiceForRange();
        }
    }

    @Override
    public Single<HttpResponse<MirrorValue<V>>> get(final String rawKey) {
        final K key = this.keyResolver.fromString(rawKey);
        final KeyQueryMetadata metadata = this.getKeyQueryMetadata(key, this.queryContext.getPointStoreName());

        // forward request if a different application is responsible for the rawKey
        if (!metadata.activeHost().equals(this.hostInfo) && !metadata.standbyHosts().contains(this.hostInfo)) {
            log.debug("Forward request to {}", metadata.activeHost());
            return Single.fromCallable(() -> this.fetch(metadata.activeHost(), key)).subscribeOn(Schedulers.io());
        }

        final ReadOnlyKeyValueStore<K, V> store = this.getReadOnlyKeyValueStore(this.pointStoreQueryParameters);

        final V value = store.get(key);
        if (value == null) {
            throw new NotFoundException(String.format("Key %s does not exist in %s", rawKey, this.topicName));
        }
        return Single.just(HttpResponse.created(new MirrorValue<>(value)).status(HttpStatus.OK));
    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getValues(final List<String> keys) {
        return Observable.fromIterable(keys)
            .flatMapSingle(this::get)
            .toList()
            .map(this::transformValuesAndCreateHttpResponse);
    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getAll() {
        // For now, we only consider the local state!
        final ReadOnlyKeyValueStore<K, V> store =
            this.streams.store(Objects.requireNonNull(this.pointStoreQueryParameters));
        return Flowable.fromIterable(store::all)
            .map(keyValue -> keyValue.value)
            .toList()
            .map(valuesList -> HttpResponse.created(new MirrorValue<>(valuesList)).status(HttpStatus.OK));

    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getRange(final String rawKey, final String from,
        final String to) {
        if (!this.rangeIndexProperties.isEnabled()) {
            throw new MirrorException("You are trying to query a range. But no range index set.",
                HttpStatus.BAD_REQUEST);
        }

        final K key = this.keyResolver.fromString(rawKey);

        final String rangeStoreName = this.rangeIndexProperties.getStoreName();
        log.debug("range store name is: {}", rangeStoreName);
        final KeyQueryMetadata metadata = this.getKeyQueryMetadata(key, rangeStoreName);

        // forward request if a different application is responsible for the rawKey
        if (!metadata.activeHost().equals(this.hostInfo) && !metadata.standbyHosts().contains(this.hostInfo)) {
            log.debug("Forward request to {}", metadata.activeHost());
            return Single.fromCallable(() -> this.fetchRange(metadata.activeHost(), key, from, to))
                .subscribeOn(Schedulers.io());
        }

        final ReadOnlyKeyValueStore<String, V> rangeStore =
            this.getReadOnlyKeyValueStore(this.rangeStoreQueryParameters);

        final List<V> values = this.queryRangeStore(key, from, to, rangeStore);

        log.debug("Fetched range from state store: {}", values);

        return Single.just(HttpResponse.created(new MirrorValue<>(values)).status(HttpStatus.OK));
    }

    private void initializeQueryServiceForRange() {
        log.debug("Initializing KafkaQueryService for range index");
        final String rangeStoreName = this.rangeIndexProperties.getStoreName();
        this.rangeStoreQueryParameters =
            StoreQueryParameters.fromNameAndType(rangeStoreName, QueryableStoreTypes.keyValueStore());

        final FieldTypeExtractor fieldTypeExtractor = this.schemaExtractor.getFieldTypeExtractor();
        final ParsedSchema parsedSchema = this.queryContext.getIndexInputStream().getValueData().getParsedSchema();

        this.rangeIndexer = ReadRangeIndexer.create(fieldTypeExtractor,
            Objects.requireNonNull(parsedSchema),
            Objects.requireNonNull(this.rangeIndexProperties.getRangeField()));
    }

    private HttpResponse<MirrorValue<V>> fetch(final HostInfo replicaHostInfo, final K key) {
        final DefaultMirrorClient<K, V> mirrorClient = this.getDefaultMirrorClient(replicaHostInfo);

        final V value = mirrorClient.fetchValue(key);

        if (value == null) {
            throw new NotFoundException("Key not found");
        }
        return HttpResponse.created(new MirrorValue<>(value))
            .header(HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS)
            .status(HttpStatus.OK);
    }

    private HttpResponse<MirrorValue<List<V>>> fetchRange(final HostInfo replicaHostInfo,
        final K key, final String from, final String to) {
        final DefaultMirrorClient<K, V> mirrorClient = this.getDefaultMirrorClient(replicaHostInfo);

        log.debug("Fetching range for key {}, from {}, to {}", key, from, to);
        final List<V> value = mirrorClient.fetchRange(key, from, to);

        if (value == null) {
            throw new NotFoundException("Key not found");
        }
        return HttpResponse.created(new MirrorValue<>(value))
            .header(HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS)
            .status(HttpStatus.OK);
    }

    private KeyQueryMetadata getKeyQueryMetadata(final K key, final String storeName) {
        try {
            final KeyQueryMetadata metadata = this.streams.queryMetadataForKey(storeName, key, this.keySerializer);
            if (metadata == null) {
                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Metadata not found");
            }

            if (metadata.equals(KeyQueryMetadata.NOT_AVAILABLE)) {
                throw new InternalErrorException("Store currently not available");
            }
            return metadata;
        } catch (final IllegalStateException exception) {
            throw new InternalErrorException("Store is not running");
        }
    }

    private <T> ReadOnlyKeyValueStore<T, V> getReadOnlyKeyValueStore(
        @Nullable final StoreQueryParameters<? extends ReadOnlyKeyValueStore<T, V>> storeQueryParameters) {

        final ReadOnlyKeyValueStore<T, V> rangeStore =
            this.streams.store(Objects.requireNonNull(storeQueryParameters));

        if (rangeStore == null) {
            final String errorMessage = String.format("Store %s not found!", storeQueryParameters.storeName());
            log.error(errorMessage);
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
        return rangeStore;
    }

    private DefaultMirrorClient<K, V> getDefaultMirrorClient(final HostInfo replicaHostInfo) {
        final String host = String.format("%s:%s", replicaHostInfo.host(), replicaHostInfo.port());
        final MirrorHost mirrorHost = MirrorHost.createWithNoPrefix(host);

        final MirrorValueParser<V> mirrorValueParser =
            new MirrorValueParser<>(this.valueResolver, this.client.objectMapper());

        return new DefaultMirrorClient<>(mirrorHost, mirrorValueParser,
            new DefaultMirrorRequestManager(this.client));
    }

    private List<V> queryRangeStore(final K key, final String from, final String to,
        final ReadOnlyKeyValueStore<String, V> rangeStore) {
        if (this.rangeIndexer == null) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create range indexer");
        }

        final String fromIndex = this.rangeIndexer.createIndex(key, from);
        final String toIndex = this.rangeIndexer.createIndex(key, to);

        log.debug("Index from is: {}", fromIndex);
        log.debug("Index to is: {}", toIndex);

        final List<V> values = new ArrayList<>();

        try (final KeyValueIterator<String, V> iterator = rangeStore.range(fromIndex, toIndex)) {
            while (iterator.hasNext()) {
                values.add(iterator.next().value);
            }
        }
        return values;
    }

    /**
     * Transforms a list of HttpResponses of MirrorValue of a specific type into a single HttpResponse of MirrorValue
     * with a list of values of that type. Furthermore, if a header is present in one of the HttpResponses (function
     * argument), an HTTP Header that informs about the Cache-Miss is set. Because of this possibility, the function
     * returns MutableHttpResponse and not just HttpResponse.
     *
     * @param listOfResponses a list of HttpResponses obtained from multiple calls to get(key)
     * @return MutableHttpResponse, possibly with a Cache-Miss Header set
     */
    private MutableHttpResponse<MirrorValue<List<V>>> transformValuesAndCreateHttpResponse(
        final Collection<? extends HttpResponse<MirrorValue<V>>> listOfResponses) {
        final boolean headerSet = listOfResponses.stream()
            .anyMatch(response -> response.header(HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER) != null);
        final List<V> values = listOfResponses.stream()
            .filter(response -> response.body() != null)
            .map(response -> response.body().getValue())
            .collect(Collectors.toList());
        final MutableHttpResponse<MirrorValue<List<V>>> responseWithoutHeader =
            HttpResponse.created(new MirrorValue<>(values)).status(HttpStatus.OK);
        if (headerSet) {
            return responseWithoutHeader.header(
                HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS);
        } else {
            return responseWithoutHeader;
        }
    }
}
