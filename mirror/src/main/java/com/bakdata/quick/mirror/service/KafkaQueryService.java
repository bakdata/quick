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

import com.bakdata.quick.common.api.client.DefaultMirrorClient;
import com.bakdata.quick.common.api.client.DefaultMirrorRequestManager;
import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.QuickConstants;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
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

    private final HttpClient client;
    private final KafkaStreams streams;
    private final HostInfo hostInfo;
    private final String storeName;
    private final String topicName;
    private final Serializer<K> keySerializer;
    private final TypeResolver<V> valueResolver;
    private final TypeResolver<K> keyResolver;
    private final StoreQueryParameters<ReadOnlyKeyValueStore<K, V>> storeQueryParameters;

    /**
     * Injectable constructor.
     *
     * @param contextProvider query service data
     */
    @Inject
    public KafkaQueryService(final QueryContextProvider contextProvider, final QuickTopicData<K, V> topicData,
                             final HttpClient client) {
        final QueryServiceContext context = contextProvider.get();
        this.client = client;
        this.streams = context.getStreams();
        this.hostInfo = context.getHostInfo();
        this.storeName = context.getStoreName();
        this.keySerializer = topicData.getKeyData().getSerde().serializer();
        this.keyResolver = topicData.getKeyData().getResolver();
        this.valueResolver = topicData.getValueData().getResolver();
        this.topicName = topicData.getName();
        this.storeQueryParameters =
            StoreQueryParameters.fromNameAndType(this.storeName, QueryableStoreTypes.keyValueStore());
    }

    @Override
    public Single<HttpResponse<MirrorValue<V>>> get(final String rawKey) {
        final K key = this.keyResolver.fromString(rawKey);
        final KeyQueryMetadata metadata;
        try {
            metadata = this.streams.queryMetadataForKey(this.storeName, key, this.keySerializer);
        } catch (final IllegalStateException exception) {
            throw new InternalErrorException("Store is not running");
        }

        if (metadata == null) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Metadata not found");
        }

        if (metadata.equals(KeyQueryMetadata.NOT_AVAILABLE)) {
            throw new InternalErrorException("Store currently not available");
        }

        // forward request if a different application is responsible for the rawKey
        if (!metadata.activeHost().equals(this.hostInfo) && !metadata.standbyHosts().contains(this.hostInfo)) {
            log.info("Forward request to {}", metadata.activeHost());
            // The cast in the second map function is possible because MutableHttpResponse is of the same type
            // as HttpResponse.
            return Single.fromCallable(() -> this.fetch(metadata.activeHost(), key)).subscribeOn(Schedulers.io());
        }

        final ReadOnlyKeyValueStore<K, V> store = this.streams.store(this.storeQueryParameters);

        if (store == null) {
            log.warn("Store {} not found!", this.storeName);
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No such store");
        }

        final V value = store.get(key);
        if (value == null) {
            throw new NotFoundException(String.format("Key %s does not exist in %s", rawKey, this.topicName));
        }

        return Single.just(HttpResponse.created(new MirrorValue<>(value)));
    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getValues(final List<String> keys) {
        return Observable.fromIterable(keys)
            .flatMapSingle(this::get)
            .toList()
            .map(listOfResponses -> {
                final boolean headerSet = listOfResponses.stream()
                    .anyMatch(response -> response.header(QuickConstants.getUpdateMappingHeader()) != null);
                final List<V> values = listOfResponses.stream()
                    .map(response -> Objects.requireNonNull(response.body()).getValue()).collect(Collectors.toList());
                if (headerSet) {
                    return HttpResponse.created(new MirrorValue<>(values)).header("x", "y");
                } else {
                    return HttpResponse.created(new MirrorValue<>(values));
                }
            });
    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getAll() {
        // For now, we only consider the local state!
        final ReadOnlyKeyValueStore<K, V> store = this.streams.store(this.storeQueryParameters);
        return Flowable.fromIterable(store::all)
            .map(keyValue -> keyValue.value)
            .toList()
            .map(e -> HttpResponse.created(new MirrorValue<>(e)));

    }

    private HttpResponse<MirrorValue<V>> fetch(final HostInfo replicaHostInfo, final K key) {
        final String host = String.format("%s:%s", replicaHostInfo.host(), replicaHostInfo.port());
        final MirrorHost mirrorHost = new MirrorHost(host, MirrorConfig.directAccess());
        final DefaultMirrorClient<K, V> mirrorClient =
            new DefaultMirrorClient<>(mirrorHost, this.client, this.valueResolver,
                new DefaultMirrorRequestManager(this.client));
        // TODO: don't bother deserializing
        final V value = mirrorClient.fetchValue(key);

        if (value == null) {
            throw new NotFoundException("Key not found");
        }
        return HttpResponse.created(new MirrorValue<>(value)).header(
            QuickConstants.getUpdateMappingHeader(), QuickConstants.getCacheUpdateMessage());
    }
}
