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

package com.bakdata.quick.common.type;

import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.schema.SchemaFormat;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import jakarta.inject.Singleton;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Default {@link ConversionProvider} for resolving the active {@link SchemaFormat}.
 */
@Singleton
public class DefaultConversionProvider implements ConversionProvider {
    private final SchemaConfig schemaConfig;
    private final KafkaConfig kafkaConfig;

    public DefaultConversionProvider(final KafkaConfig kafkaConfig, final SchemaConfig schemaConfig) {
        this.schemaConfig = schemaConfig;
        this.kafkaConfig = kafkaConfig;
    }

    @Override
    public <K> TypeResolver<K> getTypeResolver(final QuickTopicType type, @Nullable final ParsedSchema parsedSchema) {
        if (type == QuickTopicType.SCHEMA) {
            return getTypeForSchemaFormat(this.schemaConfig.getFormat()).getTypeResolver(parsedSchema);
        }
        return type.getTypeResolver(parsedSchema);
    }

    @Override
    public <K> Serde<K> getSerde(final QuickTopicType type, final boolean isKey) {
        final Map<String, String> configs = Map.of("schema.registry.url", this.kafkaConfig.getSchemaRegistryUrl());
        if (type == QuickTopicType.SCHEMA) {
            return getTypeForSchemaFormat(this.schemaConfig.getFormat()).getSerde(configs, isKey);
        }
        return type.getSerde(configs, isKey);
    }

    @Override
    public <T> Class<T> getClassType(final QuickTopicType type) {
        return type.getClassType();
    }

    private static QuickTopicType getTypeForSchemaFormat(final SchemaFormat format) {
        switch (format) {
            case AVRO:
                return QuickTopicType.AVRO;
            case PROTOBUF:
                return QuickTopicType.PROTOBUF;
            default:
                throw new IllegalArgumentException("Unknown format: " + format);
        }
    }
}
