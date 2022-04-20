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

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Configuration for internal topic registry.
 */
@ConfigurationProperties("quick.topic-registry")
@Getter
public class TopicRegistryConfig {
    private final String topicName;
    private final String serviceName;
    private final int partitions;
    private final short replicationFactor;

    /**
     * Injectable constructor.
     */
    @ConfigurationInject
    public TopicRegistryConfig(final String topicName, final String serviceName, final int partitions,
        final short replicationFactor) {
        this.topicName = topicName;
        this.serviceName = serviceName;
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
    }

    public NewTopic toNewKafkaTopic() {
        return new NewTopic(this.topicName, this.partitions, this.replicationFactor);
    }
}
