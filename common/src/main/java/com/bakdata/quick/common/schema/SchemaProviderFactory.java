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

package com.bakdata.quick.common.schema;

import com.bakdata.quick.common.condition.AvroSchemaFormatCondition;
import com.bakdata.quick.common.condition.ProtobufSchemaFormatCondition;
import com.bakdata.quick.common.config.SchemaConfig;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import javax.inject.Singleton;

/**
 * A factory for creating the correct {@link SchemaProvider}.
 *
 * <p>
 * The decision which {@link SchemaProvider} is created, is based on {@link SchemaConfig#getFormat()}.
 */
@Factory
public class SchemaProviderFactory {
    @Singleton
    @Requires(condition = AvroSchemaFormatCondition.class)
    public SchemaProvider avroSchemaProvider() {
        return new AvroSchemaProvider();
    }

    @Singleton
    @Requires(condition = ProtobufSchemaFormatCondition.class)
    public SchemaProvider protobufSchemaProvider() {
        return new ProtobufSchemaProvider();
    }
}
