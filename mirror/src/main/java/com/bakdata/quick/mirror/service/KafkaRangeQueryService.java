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
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.mirror.range.RangeIndexer;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.reactivex.Single;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

@Slf4j
public class KafkaRangeQueryService<K, V> implements RangeQueryService<V> {

    private final HttpClient client;
    private final KafkaStreams streams;
    private final HostInfo hostInfo;
    private final String storeName;
    private final String topicName;
    private final Serializer<K> keySerializer;
    private final TypeResolver<V> valueResolver;
    private final TypeResolver<K> keyResolver;
    private final StoreQueryParameters<ReadOnlyKeyValueStore<String, V>> storeQueryParameters;
    private final RangeIndexer<K, V, ?> rangeIndexer;
    private final ParsedSchema parsedSchema;
    private final String rangeField;

    /**
     * Injectable constructor.
     *
     * @param contextProvider query service data
     */
    @Inject
    public KafkaRangeQueryService(
        final QueryContextProvider contextProvider,
        final HttpClient client) {
        final QueryServiceContext context = contextProvider.get();
        this.client = client;
        final QuickTopicData<K, V> topicData = context.getTopicData();
        this.streams = context.getStreams();
        this.hostInfo = context.getHostInfo();
        this.storeName = context.getStoreName();
        this.parsedSchema = context.getTopicData().getValueData().getParsedSchema();
        this.rangeField = context.getRangeField();
        this.keySerializer = topicData.getKeyData().getSerde().serializer();
        this.keyResolver = topicData.getKeyData().getResolver();
        this.valueResolver = topicData.getValueData().getResolver();

        this.rangeIndexer = RangeIndexer.createRangeIndexer(topicData.getKeyData().getType(),
            topicData.getValueData().getType(), this.parsedSchema, this.rangeField);

        this.topicName = topicData.getName();
        this.storeQueryParameters =
            StoreQueryParameters.fromNameAndType(this.storeName, QueryableStoreTypes.keyValueStore());
    }

    @Override
    public Single<HttpResponse<MirrorValue<List<V>>>> getRange(final String rawKey, final String from,
        final String to) {
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
//        if (!metadata.activeHost().equals(this.hostInfo) && !metadata.standbyHosts().contains(this.hostInfo)) {
//            log.info("Forward request to {}", metadata.activeHost());
//            return Single.fromCallable(() -> this.fetch(metadata.activeHost(), rawKey)).subscribeOn(Schedulers.io());
//        }

        final ReadOnlyKeyValueStore<String, V> store = this.streams.store(this.storeQueryParameters);

        if (store == null) {
            log.warn("Store {} not found!", this.storeName);
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No such store");
        }

        final String fromIndex = this.rangeIndexer.createIndex(key, from);
        final String toIndex = this.rangeIndexer.createIndex(key, to);

        final List<V> values = new ArrayList<>();

        try (final KeyValueIterator<String, V> iterator = store.range(fromIndex, toIndex)) {
            while (iterator.hasNext()) {
                values.add(iterator.next().value);
            }
        }

        return Single.just(HttpResponse.created(new MirrorValue<>(values)).status(HttpStatus.OK));
    }
}
