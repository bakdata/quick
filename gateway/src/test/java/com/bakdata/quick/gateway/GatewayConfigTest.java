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

package com.bakdata.quick.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "quick.schema.path", value = "/test/path/schema.graphql")
@Property(name = "quick.kafka.bootstrap-server", value = "dummy:1234")
@Property(name = "quick.kafka.schema-registry-url", value = "http://dummy")
class GatewayConfigTest {
    @Test
    void createConfig(final GatewayConfig config) {
        assertThat(config.getPath()).isEqualTo("/test/path/schema.graphql");
    }

}
