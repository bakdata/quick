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

import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.GenericAvroResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.LongResolver;
import com.bakdata.quick.common.resolver.ProtobufResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.google.protobuf.Descriptors;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import java.util.Map;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents possible types for Quick topics.
 */
public enum QuickTopicType {
    /**
     * General schema type resolved at runtime to specific implementation.
     *
     * @see #AVRO
     * @see #PROTOBUF
     */
    SCHEMA(true) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            throw new UnsupportedOperationException("Schema's type resolver should be resolved to active format");
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            throw new UnsupportedOperationException("Schema's serde should be resolved to active format");
        }
    },

    AVRO(true) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            Objects.requireNonNull(parsedSchema, "Schema must not be null for Avro types");
            if (!(parsedSchema instanceof AvroSchema)) {
                throw new IllegalArgumentException(
                    "Expected Avro schema, but got " + parsedSchema.getClass().getName());
            }
            final Schema schema = (Schema) parsedSchema.rawSchema();
            return configuredTypeResolver(new GenericAvroResolver(schema));
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(new GenericAvroSerde(), configs, isKey);
        }
    },

    PROTOBUF(true) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            Objects.requireNonNull(parsedSchema, "Schema must not be null for Protobuf types");
            if (!(parsedSchema instanceof ProtobufSchema)) {
                throw new IllegalArgumentException(
                    "Expected Protobuf schema, but got " + parsedSchema.getClass().getName());
            }
            final Descriptors.Descriptor descriptor = ((ProtobufSchema) parsedSchema).toDescriptor();
            return configuredTypeResolver(new ProtobufResolver(descriptor));
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(new KafkaProtobufSerde<>(), configs, isKey);
        }
    },
    DOUBLE(false) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            return configuredTypeResolver(new DoubleResolver());
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(Serdes.Double(), configs, isKey);
        }
    },
    INTEGER(false) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            return configuredTypeResolver(new IntegerResolver());
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(Serdes.Integer(), configs, isKey);
        }
    },
    LONG(false) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            return configuredTypeResolver(new LongResolver());
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(Serdes.Long(), configs, isKey);
        }
    },
    STRING(false) {
        @Override
        <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema) {
            return configuredTypeResolver(new StringResolver());
        }

        @Override
        <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey) {
            return configuredSerde(Serdes.String(), configs, isKey);
        }
    };

    private final boolean isSchema;

    QuickTopicType(final boolean isSchema) {
        this.isSchema = isSchema;
    }

    public boolean isSchema() {
        return this.isSchema;
    }


    /**
     * Returns a type resolver for this type.
     *
     * @param parsedSchema schema for type resolver that is required for complex types.
     * @param <K>          inner type of the type resolver
     * @return type resolver for conversion from strings
     */
    abstract <K> TypeResolver<K> getTypeResolver(@Nullable final ParsedSchema parsedSchema);

    /**
     * Returns a configured serde for this type.
     *
     * @param configs serde configuration
     * @param isKey   true if serde is used for keys
     * @param <K>     type to be serialized from and deserialized to
     * @return configured serde
     */
    abstract <K> Serde<K> getSerde(final Map<String, ?> configs, final boolean isKey);


    @SuppressWarnings("unchecked")
    static <K> TypeResolver<K> configuredTypeResolver(final TypeResolver<?> typeResolver) {
        return (TypeResolver<K>) typeResolver;
    }

    @SuppressWarnings("unchecked")
    static <K> Serde<K> configuredSerde(final Serde<?> serde, final Map<String, ?> config, final boolean isKey) {
        serde.configure(config, isKey);
        return (Serde<K>) serde;
    }

}
