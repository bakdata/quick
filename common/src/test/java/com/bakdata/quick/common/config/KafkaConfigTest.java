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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KafkaConfigTest {
    @Test
    void shouldCreateConfig() {
        final Map<String, Object> properties = Map.of(
                "quick.kafka.bootstrap.server", "localhost:9092",
                "quick.kafka.schema.registry.url", "https://localhost:8081",
                "quick.kafka.application.id", "testId"
        );

        final KafkaConfig config = ConfigUtils.createWithProperties(properties, KafkaConfig.class);

        assertThat(config.getBootstrapServer()).isEqualTo("localhost:9092");
        assertThat(config.getSchemaRegistryUrl()).isEqualTo("https://localhost:8081");
        assertThat(config.getApplicationId()).isEqualTo(Optional.of("testId"));
    }


    @Test
    void shouldCreateConfigFromEnv() {
        final Map<String, Object> env = Map.of(
                "QUICK_KAFKA_BOOTSTRAP_SERVER", "localhost:9092",
                "QUICK_KAFKA_SCHEMA_REGISTRY_URL", "https://localhost:8081",
                "QUICK_KAFKA_APPLICATION_ID", "testId"
        );

        final KafkaConfig config = ConfigUtils.createWithEnvironment(env, KafkaConfig.class);

        assertThat(config.getBootstrapServer()).isEqualTo("localhost:9092");
        assertThat(config.getSchemaRegistryUrl()).isEqualTo("https://localhost:8081");
        assertThat(config.getApplicationId()).isEqualTo(Optional.of("testId"));
    }

    @Test
    void shouldCreateConfigWithoutAppId() {
        final Map<String, Object> env = Map.of(
                "QUICK_KAFKA_BOOTSTRAP_SERVER", "localhost:9092",
                "QUICK_KAFKA_SCHEMA_REGISTRY_URL", "https://localhost:8081"
        );

        final KafkaConfig config = ConfigUtils.createWithEnvironment(env, KafkaConfig.class);

        assertThat(config.getBootstrapServer()).isEqualTo("localhost:9092");
        assertThat(config.getSchemaRegistryUrl()).isEqualTo("https://localhost:8081");
        assertThat(config.getApplicationId()).isNotPresent();
    }
}
