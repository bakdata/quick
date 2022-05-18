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

package com.bakdata.quick.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import com.bakdata.quick.common.schema.SchemaFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaConfigTest {

    @Test
    void shouldCreateConfigWithProtobuf() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "protobuf");
        final SchemaConfig schemaConfig = ConfigUtils.createWithProperties(properties, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldReadFormatCaseInsensitive() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "ProtoBuf");
        final SchemaConfig schemaConfig = ConfigUtils.createWithProperties(properties, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.PROTOBUF);
    }


    @Test
    void shouldCreateConfigFromCaps() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "PROTOBUF");
        final SchemaConfig schemaConfig = ConfigUtils.createWithProperties(properties, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigWithDefault() {
        final Map<String, Object> properties = Map.of(
            "quick.schema.avro.namespace", "test"
        );
        final SchemaConfig schemaConfig = ConfigUtils.createWithProperties(properties, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.AVRO);
    }

    @Test
    void shouldCreateConfigFromEnvVariable() {
        final Map<String, Object> env = Map.of(
            "QUICK_SCHEMA_FORMAT", "protobuf"
        );
        final SchemaConfig schemaConfig = ConfigUtils.createWithEnvironment(env, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigFromEnvVariableCaps() {
        final Map<String, Object> env = Map.of(
            "QUICK_SCHEMA_FORMAT", "PROTOBUF"
        );
        final SchemaConfig schemaConfig = ConfigUtils.createWithEnvironment(env, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigFromEnvVariableAvro() {
        final Map<String, Object> env = Map.of(
            "QUICK_SCHEMA_FORMAT", "Avro"
        );
        final SchemaConfig schemaConfig = ConfigUtils.createWithEnvironment(env, SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(SchemaFormat.AVRO);
    }

}
