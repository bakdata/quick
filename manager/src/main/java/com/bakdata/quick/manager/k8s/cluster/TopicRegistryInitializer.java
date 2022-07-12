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

package com.bakdata.quick.manager.k8s.cluster;

import static com.bakdata.quick.common.api.model.KeyValueEnum.VALUE;

import com.bakdata.quick.common.api.model.AvroTopicData;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.TopicRegistryConfig;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.manager.mirror.MirrorService;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.TopicExistsException;

/**
 * Ensures deployment of a Topic Registry at start up.
 */
@Requires(property = "quick.manager.create-topic-registry", value = StringUtils.TRUE)
@Singleton
@Slf4j
public class TopicRegistryInitializer {
    private final KafkaConfig kafkaConfig;
    private final SchemaRegistryClient schemaRegistryClient;
    private final TopicRegistryConfig topicRegistryConfig;
    private final MirrorService mirrorService;

    /**
     * Injectable constructor.
     */
    @Inject
    public TopicRegistryInitializer(final KafkaConfig kafkaConfig, final TopicRegistryConfig topicRegistryConfig,
        final MirrorService mirrorService) {
        this.kafkaConfig = kafkaConfig;
        this.schemaRegistryClient = new CachedSchemaRegistryClient(kafkaConfig.getSchemaRegistryUrl(), 1_000);
        this.topicRegistryConfig = topicRegistryConfig;
        this.mirrorService = mirrorService;
    }

    /**
     * Ensures the topic registry itself and its topic are created.
     */
    @EventListener
    @Async
    public void onStartUp(final StartupEvent event) {
        // the topic registry is a basically just a mirror application
        // therefore, it needs its own kafka topic
        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaConfig.getBootstrapServer());

        boolean registryExists = false;

        try (final AdminClient admin = AdminClient.create(properties)) {
            final NewTopic immutableTopic = this.topicRegistryConfig.toNewKafkaTopic();
            admin.createTopics(List.of(immutableTopic));
        } catch (final TopicExistsException ignored) {
            log.info("Internal registry topic already exists");
            registryExists = true;
        } catch (final KafkaException e) {
            throw new InternalErrorException("Kafka could not be reached: " + e.getMessage());
        }

        // register the avro schema of the topic data class with the schema registry
        try {
            final String subject = VALUE.asSubject(this.topicRegistryConfig.getTopicName());
            final ParsedSchema topicDataSchema = new AvroSchema(AvroTopicData.getClassSchema());
            this.schemaRegistryClient.register(subject, topicDataSchema);
        } catch (final IOException | RestClientException exception) {
            if (!registryExists) {
                throw new InternalErrorException("Could not register schema for internal topic registry topic");
            }
        }

        final MirrorCreationData topicRegistryCreationData = new MirrorCreationData(
            this.topicRegistryConfig.getServiceName(),
            this.topicRegistryConfig.getTopicName(),
            1,
            null, // null means we use the default tag
            null
        );

        // create topic-registry mirror
        this.mirrorService.createInternalMirror(topicRegistryCreationData) // no retention time
            .subscribe(
                () -> log.info("Deployed internal topic-registry service"),
                e -> log.info("Could not deploy internal topic-registry service: {}", e.getMessage()))
            .dispose();
    }
}
