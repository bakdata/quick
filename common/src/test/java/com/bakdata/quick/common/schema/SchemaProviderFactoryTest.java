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

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaProviderFactoryTest {

    @Test
    void shouldCreateAvroSchemaProvider() {
        final SchemaProvider schemaProvider =
            ConfigUtils.createWithProperties(Map.of("quick.schema.format", "avro"), SchemaProvider.class);
        assertThat(schemaProvider).isInstanceOf(AvroSchemaProvider.class);
    }

    @Test
    void shouldCreateProtobufSchemaProvider() {
        final SchemaProvider schemaProvider =
            ConfigUtils.createWithProperties(Map.of("quick.schema.format", "protobuf"), SchemaProvider.class);
        assertThat(schemaProvider).isInstanceOf(ProtobufSchemaProvider.class);
    }

}
