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

package com.bakdata.quick.common.type.inferer;

import com.bakdata.quick.common.api.model.TopicSchemaTypes;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.schema.SchemaFetcher;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import io.micronaut.caffeine.cache.Cache;
import io.micronaut.caffeine.cache.Caffeine;
import io.reactivex.Single;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * TopicTypeService that infers the types itself.
 *
 * <p>
 * This class reads a chunk of a Kafka topic and based on the returned values, guesses the containing data.
 */
public class InferTopicTypeService implements TopicTypeService {
    private final Cache<String, TopicSchemaTypes> localCache;
    private final String bootstrapServer;
    private final String appId;
    private final SchemaFetcher schemaFetcher;

    /**
     * Constructor.
     *
     * @param bootstrapServer kafka server
     * @param appId           id to use when connecting to kafka
     * @param schemaFetcher   schema registry client
     */
    public InferTopicTypeService(final String bootstrapServer, final String appId, final SchemaFetcher schemaFetcher) {
        this.localCache = Caffeine.newBuilder().build();
        this.bootstrapServer = bootstrapServer;
        this.appId = appId;
        this.schemaFetcher = schemaFetcher;
    }

    @Override
    public <K, V> Single<QuickTopicData<K, V>> getTopicData(final String topic) {
        TopicSchemaTypes types = this.localCache.getIfPresent(topic);
        if (types == null) {
            final Optional<TopicSchemaTypes> optionalTypes = this.inferSerDes(topic);
            if (optionalTypes.isEmpty()) {
                return Single.error(new IllegalStateException("Topic is empty. Cannot infer types."));
            }
            types = optionalTypes.get();
            this.localCache.put(topic, types);
        }
        final QuickData<K> keyInfo = this.fromType(types.getKeyType(), topic, true);
        final QuickData<V> valueInfo = this.fromType(types.getValueType(), topic, false);
        return Single.just(new QuickTopicData<>(topic, TopicWriteType.MUTABLE, keyInfo, valueInfo));
    }

    private static QuickTopicType inferSerDe(final Collection<byte[]> bytes) {
        if (isInteger(bytes)) {
            return QuickTopicType.INTEGER;
        } else if (isLong(bytes)) {
            return QuickTopicType.LONG;
        } else if (isGenericAvro(bytes)) {
            return QuickTopicType.SCHEMA;
        } else {
            return QuickTopicType.STRING;
        }
    }

    private static boolean isGenericAvro(final Collection<byte[]> bytes) {
        // avro uses a 0 byte as identifier
        // the length must be >= 5 because the identifier and the id (4 bytes) take 5 bytes
        return bytes.stream().map(array -> array[0]).allMatch(i -> i == 0x0)
            && bytes.stream().map(Array::getLength).allMatch(i -> i > 5);
    }

    private static boolean isLong(final Collection<byte[]> bytes) {
        return bytes.stream().map(Array::getLength).allMatch(i -> i == 8);
    }

    private static boolean isInteger(final Collection<byte[]> bytes) {
        return bytes.stream().map(Array::getLength).allMatch(i -> i == 4);
    }

    private Optional<TopicSchemaTypes> inferSerDes(final String topic) {
        final InferConsumer inferConsumer = new InferConsumer(this.bootstrapServer, topic, this.appId);
        final ConsumerRecords<byte[], byte[]> records = inferConsumer.fetchRecords();

        if (records.isEmpty()) {
            return Optional.empty();
        }

        final List<byte[]> keys = new ArrayList<>();
        final List<byte[]> values = new ArrayList<>();
        for (final ConsumerRecord<byte[], byte[]> record : records) {
            keys.add(record.key());
            values.add(record.value());
        }

        final QuickTopicType keyType = inferSerDe(keys);
        final QuickTopicType valueType = inferSerDe(values);
        return Optional.of(new TopicSchemaTypes(keyType, valueType));
    }

    @SuppressWarnings("unchecked")
    private <T> QuickData<T> fromType(final QuickTopicType type, final String topic, final boolean isKey) {
        final QuickData<?> info = new QuickData<>(type, type.getSerde(), type.getTypeResolver());
        if (info.getType() == QuickTopicType.SCHEMA) {
            final Single<Schema> schema = isKey ? this.schemaFetcher.getKeySchema(topic)
                : this.schemaFetcher.getValueSchema(topic);
            info.getResolver().configure(schema.blockingGet());
            // TODO configure serde with url
        }
        return (QuickData<T>) info;
    }
}
