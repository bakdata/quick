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

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.model.AvroTopicData;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.TopicRegistryConfig;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.manager.mirror.MirrorService;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.StartupEvent;
import io.reactivex.Completable;
import java.io.IOException;
import java.util.UUID;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.TopicConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TopicRegistryInitializerTest {
    private static final String TEST_NAME = "internal-test";
    private static EmbeddedKafkaCluster kafkaCluster = null;
    @RegisterExtension
    final SchemaRegistryMock schemaRegistry = new SchemaRegistryMockExtension();
    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final MirrorService mirrorServiceMock = mock(MirrorService.class);

    @BeforeAll
    static void beforeAll() {
        kafkaCluster = provisionWith(defaultClusterConfig());
        kafkaCluster.start();
    }

    @AfterAll
    static void afterAll() {
        kafkaCluster.stop();
    }

    @Test
    void shouldCreateTopicRegistryTopic() {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);
        topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));

        assertThat(kafkaCluster.exists(topicName)).isTrue();
    }

    @Test
    void shouldCreateSchema() throws IOException, RestClientException {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);
        topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));

        final SchemaRegistryClient registryClient = this.schemaRegistry.getSchemaRegistryClient();
        final String subject = topicName + "-value";

        assertThat(registryClient.getAllSubjects()).containsExactly(subject);
        final ParsedSchema topicDataSchema = new AvroSchema(AvroTopicData.getClassSchema());
        assertThat(registryClient.getSchemaBySubjectAndId(subject, 1)).isEqualTo(topicDataSchema);
    }

    @Test
    void shouldCreateMirror() {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);
        topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));

        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TEST_NAME,
            topicName,
            1,
            null,
            null);
        verify(this.mirrorServiceMock).createInternalMirror(mirrorCreationData);
    }

    @Test
    void shouldNotFailIfTopicRegistryExists() {
        final String topicName = UUID.randomUUID().toString();
        this.failingMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);

        assertThatCode(() -> {
            topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldNotFailIfTopicExists() {
        final String topicName = UUID.randomUUID().toString();
        kafkaCluster.createTopic(TopicConfig.withName(topicName).useDefaults());

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);

        assertThatCode(() -> {
            topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldNotFailIfSchemaExists() {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);

        assertThatCode(() -> {
            topicRegistryInitializer.onStartUp(new StartupEvent(this.applicationContext));
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldFailIfKafkaIsNotAvailable() {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig("nope:9092", this.schemaRegistry.getUrl());
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);

        final StartupEvent event = new StartupEvent(this.applicationContext);
        assertThatExceptionOfType(InternalErrorException.class).isThrownBy(() ->
            topicRegistryInitializer.onStartUp(event)
        );
    }

    @Test
    void shouldFailIfSchemaRegistryIsNotAvailable() {
        final String topicName = UUID.randomUUID().toString();

        this.successfulMock();
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), "http://nope:9092");
        final TopicRegistryConfig registryConfig = new TopicRegistryConfig(topicName, TEST_NAME, 3, (short) 1);

        final TopicRegistryInitializer
            topicRegistryInitializer = new TopicRegistryInitializer(kafkaConfig, registryConfig, this.mirrorServiceMock);

        final StartupEvent startupEvent = new StartupEvent(this.applicationContext);
        assertThatExceptionOfType(InternalErrorException.class).isThrownBy(() ->
            topicRegistryInitializer.onStartUp(startupEvent)
        );
    }

    private void successfulMock() {
        when(this.mirrorServiceMock.createInternalMirror(any())).thenReturn(Completable.complete());
    }

    private void failingMock() {
        when(this.mirrorServiceMock.createInternalMirror(any()))
            .thenReturn(Completable.error(new RuntimeException("Something failed!")));
    }
}
