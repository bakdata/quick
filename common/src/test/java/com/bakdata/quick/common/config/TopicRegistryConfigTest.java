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
import org.junit.jupiter.api.Test;

class TopicRegistryConfigTest {
    @Test
    void shouldCreateTopicRegistryConfig() {

        final Map<String, Object> properties = Map.of(
                "quick.topic-registry.topicName", "test-topic",
                "quick.topic-registry.serviceName", "test-service",
                "quick.topic-registry.partitions", "3",
                "quick.topic-registry.replicationFactor", "1"
        );

        final TopicRegistryConfig config = ConfigUtils.createWithProperties(properties, TopicRegistryConfig.class);

        assertThat(config.getTopicName()).isEqualTo("test-topic");
        assertThat(config.getServiceName()).isEqualTo("test-service");
        assertThat(config.getPartitions()).isEqualTo(3);
        assertThat(config.getReplicationFactor()).isEqualTo((short) 1);
    }


    @Test
    void shouldCreateTopicRegistryConfigFromEnv() {

        final Map<String, Object> properties = Map.of(
                "QUICK_TOPIC_REGISTRY_TOPIC_NAME", "test-topic",
                "QUICK_TOPIC_REGISTRY_SERVICE_NAME", "test-service",
                "QUICK_TOPIC_REGISTRY_PARTITIONS", "3",
                "QUICK_TOPIC_REGISTRY_REPLICATION_FACTOR", "1"
        );

        final TopicRegistryConfig config = ConfigUtils.createWithEnvironment(properties, TopicRegistryConfig.class);

        assertThat(config.getTopicName()).isEqualTo("test-topic");
        assertThat(config.getServiceName()).isEqualTo("test-service");
        assertThat(config.getPartitions()).isEqualTo(3);
        assertThat(config.getReplicationFactor()).isEqualTo((short) 1);
    }
}
