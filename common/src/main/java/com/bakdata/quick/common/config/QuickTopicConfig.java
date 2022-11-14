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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Getter;

/**
 * Config class for kafka topics.
 *
 * <p>
 * It sets the default values for newly created topics.
 */
@ConfigurationProperties("quick.kafka.internal")
@Getter
public class QuickTopicConfig {
    private final int partitions;
    private final short replicationFactor;

    /**
     * Constructor initializing config with default values.
     */
    public QuickTopicConfig() {
        this.partitions = 10;
        this.replicationFactor = 3;
    }

    /**
     * Injectable constructor.
     *
     * @param partitions        number of partitions for topics. It must be to >=1.
     * @param replicationFactor number of replicas per topic. It must be between 1 and 16. Note that it cannot be
     *                          greater than the number of brokers.
     */
    @ConfigurationInject
    public QuickTopicConfig(@Min(1) final int partitions, @Min(1) @Max(16) final short replicationFactor) {
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
    }
}
