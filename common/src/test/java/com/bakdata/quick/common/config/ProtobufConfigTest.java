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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import io.micronaut.context.ApplicationContext;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProtobufConfigTest {

    @Test
    void shouldReadNamespace() {
        final Map<String, Object> properties =
            Map.of("quick.schema.format", "protobuf", "quick.schema.protobuf.package", "test");
        final ProtobufConfig config = ConfigUtils.createWithProperties(properties, ProtobufConfig.class);
        assertThat(config.getProtobufPackage()).isEqualTo("test");
    }

    @Test
    void shouldNotExistIfFormatIsProtobuf() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "avro");
        final Optional<ProtobufConfig> config;
        try (final ApplicationContext context = ConfigUtils.createWithProperties(properties)) {
            config = context.findBean(ProtobufConfig.class);
            assertThat(config).isEmpty();
        }
    }
}
