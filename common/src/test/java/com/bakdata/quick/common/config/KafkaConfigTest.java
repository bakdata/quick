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

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "quick.kafka.bootstrap.server", value = "localhost:9092")
@Property(name = "quick.kafka.schema.registry.url", value = "https://localhost:8081")
@Property(name = "quick.kafka.application.id", value = "testId")
class KafkaConfigTest {
    @Test
    void createConfig(final KafkaConfig config) {
        assertThat(config.getBootstrapServer()).isEqualTo("localhost:9092");
        assertThat(config.getSchemaRegistryUrl()).isEqualTo("https://localhost:8081");
        assertThat(config.getApplicationId()).isEqualTo(Optional.of("testId"));
    }
}
