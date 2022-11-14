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
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.apache.kafka.clients.CommonClientConfigs;

/**
 * Configuration for Kafka.
 */
@ConfigurationProperties("quick.kafka")
@Getter
public class KafkaConfig {
    public static final String BOOTSTRAP_SERVER_ENV = "QUICK_KAFKA_BOOTSTRAP_SERVER";
    public static final String SCHEMA_REGISTRY_URL_ENV = "QUICK_KAFKA_SCHEMA_REGISTRY_URL";
    public static final String APP_ID_ENV = "QUICK_KAFKA_APPLICATION_ID";

    private final String bootstrapServer;
    private final String schemaRegistryUrl;
    private final Optional<String> applicationId; // not required

    /**
     * Constructor visible for testing.
     *
     * @param bootstrapServer   kafka server address
     * @param schemaRegistryUrl address of schema registry
     */
    public KafkaConfig(final String bootstrapServer, final String schemaRegistryUrl) {
        this.bootstrapServer = bootstrapServer;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.applicationId = Optional.empty();
    }

    /**
     * Injectable constructor.
     *
     * @param bootstrapServer   kafka server address
     * @param schemaRegistryUrl address of schema registry
     * @param applicationId     id used by the application when interacting with kafka
     */
    @ConfigurationInject
    public KafkaConfig(final String bootstrapServer, final String schemaRegistryUrl,
        final Optional<String> applicationId) {
        this.bootstrapServer = bootstrapServer;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.applicationId = applicationId;
    }

    /**
     * Converts this to a map for configuring Kafka entities.
     */
    public Map<String, Object> asProps() {
        return Map.of(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServer,
            "schema.registry.url", this.schemaRegistryUrl
        );
    }
}
