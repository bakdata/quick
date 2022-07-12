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

package com.bakdata.quick.common;

import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.schema.SchemaFormat;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.DefaultConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.reactivex.Single;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Serde;

/**
 * Mock for topic type service.
 */
public class TestTopicTypeService implements TopicTypeService {
    private final Supplier<String> urlSupplier;
    private final QuickTopicType keyType;
    private final QuickTopicType valueType;
    private final ParsedSchema keySchema;
    private final ParsedSchema valueSchema;
    private final ConversionProvider conversionProvider;

    /**
     * Constructor for builder.
     */
    @Builder
    public TestTopicTypeService(final Supplier<String> urlSupplier, final QuickTopicType keyType,
                                final QuickTopicType valueType, @Nullable final Schema keySchema,
                                @Nullable final Schema valueSchema, final ConversionProvider conversionProvider) {
        this.urlSupplier = urlSupplier;
        this.keyType = keyType;
        this.valueType = valueType;
        this.keySchema = keySchema == null ? null : new AvroSchema(keySchema.toString());
        this.valueSchema = valueSchema == null ? null : new AvroSchema(valueSchema.toString());
        this.conversionProvider = conversionProvider == null ? avroConversionProvider() : conversionProvider;
    }

    @Override
    public <K, V> Single<QuickTopicData<K, V>> getTopicData(final String topic) {
        final String schemaRegistryUrl = this.urlSupplier.get();
        final Map<String, String> configs = Map.of("schema.registry.url", schemaRegistryUrl);
        final Serde<K> keySerde = this.conversionProvider.getSerde(this.keyType, configs, true);
        final Serde<V> valueSerde = this.conversionProvider.getSerde(this.valueType, configs, false);
        final TypeResolver<K> keyResolver = this.conversionProvider.getTypeResolver(this.keyType, this.keySchema);
        final TypeResolver<V> valueResolver = this.conversionProvider.getTypeResolver(this.valueType, this.valueSchema);
        final QuickData<K> quickData = new QuickData<>(this.keyType, keySerde, keyResolver);
        final QuickData<V> valueInfo = new QuickData<>(this.valueType, valueSerde, valueResolver);
        final QuickTopicData<K, V> topicInfo =
            new QuickTopicData<>(topic, TopicWriteType.MUTABLE, quickData, valueInfo);
        return Single.just(topicInfo);
    }


    public static ConversionProvider avroConversionProvider() {
        final SchemaConfig schemaConfig = new SchemaConfig(Optional.of(SchemaFormat.AVRO), Optional.empty());
        return new DefaultConversionProvider(schemaConfig);
    }

    public static ConversionProvider protoConversionProvider() {
        final SchemaConfig schemaConfig = new SchemaConfig(Optional.of(SchemaFormat.PROTOBUF), Optional.empty());
        return new DefaultConversionProvider(schemaConfig);
    }
}
