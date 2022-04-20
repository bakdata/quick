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
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "quick.topic-registry.topicName", value = "test-topic")
@Property(name = "quick.topic-registry.serviceName", value = "test-service")
@Property(name = "quick.topic-registry.partitions", value = "3")
@Property(name = "quick.topic-registry.replicationFactor", value = "1")
class TopicRegistryConfigTest {
    @Test
    void shouldCreateTopicRegistryConfig(final TopicRegistryConfig config) {
        assertThat(config.getTopicName()).isEqualTo("test-topic");
        assertThat(config.getServiceName()).isEqualTo("test-service");
        assertThat(config.getPartitions()).isEqualTo(3);
        assertThat(config.getReplicationFactor()).isEqualTo((short) 1);
    }
}
