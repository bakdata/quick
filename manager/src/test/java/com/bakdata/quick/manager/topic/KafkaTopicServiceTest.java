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

package com.bakdata.quick.manager.topic;

import static com.bakdata.quick.manager.TestUtil.createDefaultTopicCreationData;
import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.TestTopicRegistryClient;
import com.bakdata.quick.common.api.client.gateway.GatewayClient;
import com.bakdata.quick.common.api.client.mirror.TopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.api.model.manager.GatewaySchema;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.api.model.manager.creation.TopicCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.ProtobufConfig;
import com.bakdata.quick.common.config.QuickTopicConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.exception.QuickException;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.manager.gateway.GatewayService;
import com.bakdata.quick.manager.graphql.GraphQLToAvroConverter;
import com.bakdata.quick.manager.graphql.GraphQLToProtobufConverter;
import com.bakdata.quick.manager.mirror.MirrorService;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.exceptions.HttpException;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class KafkaTopicServiceTest {
    public static final QuickTopicConfig TOPIC_CONFIG = new QuickTopicConfig(3, (short) 1);
    private static final GatewaySchema GATEWAY_SCHEMA = new GatewaySchema("test", "Test");
    private static final String SCHEMA = "type Test { id: String! }";

    private static EmbeddedKafkaCluster kafkaCluster = null;
    private final SchemaRegistryMock schemaRegistry =
        new SchemaRegistryMock(List.of(new AvroSchemaProvider(), new ProtobufSchemaProvider()));

    private final MirrorService mirrorService = mock(MirrorService.class);
    private final GatewayClient gatewayClient = mock(GatewayClient.class);

    private final GatewayService gatewayService = mock(GatewayService.class);
    private final TopicRegistryClient topicRegistryClient = new TestTopicRegistryClient();
    private final GraphQLToAvroConverter graphQLToAvroConverter = new GraphQLToAvroConverter("test.avro");
    private final GraphQLToProtobufConverter graphQLToProtobufConverter =
        new GraphQLToProtobufConverter(new ProtobufConfig("test"));

    @BeforeAll
    static void beforeAll() {
        kafkaCluster = provisionWith(defaultClusterConfig());
        kafkaCluster.start();
    }

    @AfterAll
    static void afterAll() {
        kafkaCluster.stop();
    }

    @BeforeEach
    void setUp() {
        this.schemaRegistry.start();
    }

    @AfterEach
    void tearDown() {
        this.schemaRegistry.stop();
    }

    @Test
    void shouldCreateTopic() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final TopicCreationData requestData = createDefaultTopicCreationData(null);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData);

        assertThat(completable.blockingGet()).isNull();
        assertThat(kafkaCluster.exists(topicName)).isTrue();
    }

    @Test
    void shouldNotCreateTopicThatAlreadyExists() {
        final String topicName = UUID.randomUUID().toString();
        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final TopicCreationData requestData = createDefaultTopicCreationData(null);
        topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData)
            .blockingAwait();

        assertThat(kafkaCluster.exists(topicName)).isTrue();

        final Throwable exception =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData)
                .blockingGet();

        final String expectedErrorMsg = String.format("Topic \"%s\" already exists", topicName);
        assertThat(exception).isExactlyInstanceOf(BadArgumentException.class)
            .extracting(Throwable::getMessage).asString()
            .startsWith(expectedErrorMsg);
    }

    @Test
    void shouldNotCreateTopicThatAlreadyExistsInRegistry() throws RestClientException, IOException {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final TopicCreationData requestData = createDefaultTopicCreationData(null);

        this.topicRegistryClient
            .register(topicName, new TopicData(topicName, TopicWriteType.MUTABLE, null, null, null))
            .blockingAwait();

        assertThat(this.topicRegistryClient.topicDataExists(topicName).blockingGet()).isTrue();

        final Throwable exception =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData)
                .blockingGet();

        final String expectedErrorMsg = String.format("Topic \"%s\" already exists", topicName);
        assertThat(exception).isExactlyInstanceOf(BadArgumentException.class)
            .extracting(Throwable::getMessage).asString()
            .startsWith(expectedErrorMsg);

        assertThat(kafkaCluster.exists(topicName)).isFalse();
        assertThat(this.schemaRegistry.getSchemaRegistryClient().getAllSubjects())
            .isEmpty();
    }

    @Test
    void shouldRegisterTopic() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final TopicCreationData requestData = createDefaultTopicCreationData(null);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData);

        final TopicData expectedTopicData =
            new TopicData(topicName, TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE,
                null);
        assertThat(completable.blockingGet()).isNull();
        assertThat(this.topicRegistryClient.getTopicData(topicName).blockingGet())
            .usingRecursiveComparison()
            .isEqualTo(expectedTopicData);
    }

    @Test
    @Disabled("Compatibility test not supported in SR mock yet")
    void shouldNotCreateTopicIfSubjectExists() throws RestClientException, IOException {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        this.schemaRegistry.registerValueSchema(topicName, this.graphQLToAvroConverter.convert(SCHEMA));

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.just(new SchemaData(SCHEMA)));
        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Throwable throwable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.AVRO, requestData)
                .blockingGet();

        assertThat(throwable)
            .isNotNull()
            .isExactlyInstanceOf(BadArgumentException.class)
            .extracting(Throwable::getMessage).asString()
            .endsWith("already exists");

        assertThat(kafkaCluster.exists(topicName)).isFalse();
        assertThat(this.topicRegistryClient.topicDataExists(topicName).blockingGet()).isFalse();
        assertThat(this.schemaRegistry.getSchemaRegistryClient().getAllSubjects()).isEmpty();
    }

    @Test
    void shouldRegisterTopicGraphQLSchema() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.just(new SchemaData(SCHEMA)));
        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.AVRO, requestData);

        assertThat(completable.blockingGet()).isNull();
        final TopicData expected =
            new TopicData(topicName, TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.AVRO, SCHEMA);
        assertThat(this.topicRegistryClient.getTopicData(topicName).blockingGet())
            .usingRecursiveComparison()
            .isEqualTo(expected);
    }

    @Test
    void shouldNotCreateTopicWithInvalidGraphQLSchema() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.error(new BadArgumentException("Type OopsNotHere does not exist")));
        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Throwable throwable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.AVRO, requestData)
                .blockingGet();

        assertThat(throwable).isNotNull()
            .isExactlyInstanceOf(BadArgumentException.class)
            .extracting(Throwable::getMessage).asString()
            .startsWith("Type OopsNotHere does not exist");
    }

    @Test
    void shouldRegisterTopicAvroSchema() throws IOException, RestClientException {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.just(new SchemaData(SCHEMA)));
        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.AVRO, requestData);

        assertThat(completable.blockingGet()).isNull();

        final SchemaRegistryClient schemaRegistryClient = this.schemaRegistry.getSchemaRegistryClient();
        final String subject = topicName + "-value";
        final ParsedSchema expectedSchema = this.graphQLToAvroConverter.convert(SCHEMA);

        assertThat(schemaRegistryClient.getAllSubjects()).containsExactly(subject);
        assertThat(schemaRegistryClient.getSchemaById(1)).isEqualTo(expectedSchema);
    }

    @Test
    void shouldRegisterTopicProtoSchema() throws IOException, RestClientException {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForProto();
        this.setupSuccessfulMock();

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.just(new SchemaData(SCHEMA)));
        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.PROTOBUF, requestData);

        assertThat(completable.blockingGet()).isNull();

        final SchemaRegistryClient schemaRegistryClient = this.schemaRegistry.getSchemaRegistryClient();
        final String subject = topicName + "-value";
        final ParsedSchema expectedSchema = this.graphQLToProtobufConverter.convert(SCHEMA);

        assertThat(schemaRegistryClient.getAllSubjects()).containsExactly(subject);
        assertThat(schemaRegistryClient.getSchemaById(1)).isEqualTo(expectedSchema);
    }

    @Test
    void shouldSetRetentionTime() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.just(new GatewayDescription("test", 1, "latest")));
        when(this.gatewayClient.getWriteSchema(anyString(), anyString()))
            .thenReturn(Single.just(new SchemaData(SCHEMA)));

        final Duration retentionTime = Duration.ofMinutes(30);
        final TopicCreationData requestData =
            new TopicCreationData(TopicWriteType.MUTABLE, GATEWAY_SCHEMA, null, retentionTime, true, null);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData);

        assertThat(completable.blockingGet()).isNull();

        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            topicName,
            topicName,
            1,
            null,
            retentionTime,
            null);

        verify(this.mirrorService).createMirror(mirrorCreationData);
    }

    @Test
    void shouldDeleteTopicFromTopicRegistry() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final TopicCreationData requestData =
            new TopicCreationData(TopicWriteType.MUTABLE, null, null, null, true, null);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData);

        assertThat(completable.blockingGet()).isNull();
        assertThat(topicService.deleteTopic(topicName).blockingGet()).isNull();

        verify(this.mirrorService).deleteMirror(topicName);
        assertThatNullPointerException().isThrownBy(() -> this.topicRegistryClient.getTopicData(topicName));
    }

    @Test
    void shouldRetrieveAllTopics() {

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        final int numberOfTopics = 10;

        for (int i = 0; i < numberOfTopics; i++) {
            final String topicName = UUID.randomUUID().toString();
            final TopicCreationData requestData =
                new TopicCreationData(TopicWriteType.MUTABLE, null, null, null, true, null);
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.DOUBLE, requestData)
                .blockingAwait();
        }

        final Single<List<TopicData>> allTopics = this.topicRegistryClient.getAllTopics();
        assertThat(allTopics.blockingGet().size()).isEqualTo(numberOfTopics);
    }

    @Test
    void shouldThrowExceptionForNonExistingGateway() {
        final String topicName = UUID.randomUUID().toString();

        final TopicService topicService = this.newTopicServiceForAvro();
        this.setupSuccessfulMock();

        // error thrown when checking if gateway exists
        final Throwable error =
            new NotFoundException(String.format("Could not find resource %s", GATEWAY_SCHEMA.getGateway()));
        when(this.gatewayService.getGateway(GATEWAY_SCHEMA.getGateway()))
            .thenReturn(Single.error(error));

        // resource doesn't exist, traefik cannot route it; This should not be called since we check existence before
        final HttpException clientException = new HttpClientResponseException("Not found", HttpResponse.notFound());
        when(this.gatewayClient.getWriteSchema(anyString(), anyString())).thenReturn(Single.error(clientException));

        final TopicCreationData requestData = createDefaultTopicCreationData(GATEWAY_SCHEMA);
        final Completable completable =
            topicService.createTopic(topicName, QuickTopicType.DOUBLE, QuickTopicType.AVRO, requestData);

        final Throwable actual = completable.blockingGet();
        assertThat(actual)
            .isNotNull()
            .isInstanceOf(QuickException.class)
            .hasMessageStartingWith("Could not find resource test");

        verify(this.gatewayClient, never()).getWriteSchema(anyString(), anyString());
    }

    private TopicService newTopicServiceForAvro() {
        return new KafkaTopicService(this.topicRegistryClient, this.gatewayClient, this.graphQLToAvroConverter,
            this.mirrorService, this.gatewayService, TOPIC_CONFIG, this.newKafkaConfig());
    }

    private TopicService newTopicServiceForProto() {
        return new KafkaTopicService(this.topicRegistryClient, this.gatewayClient, this.graphQLToProtobufConverter,
            this.mirrorService, this.gatewayService, TOPIC_CONFIG, this.newKafkaConfig());
    }

    private KafkaConfig newKafkaConfig() {
        return new KafkaConfig(kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
    }

    private void setupSuccessfulMock() {

        when(this.mirrorService.createMirror(any()))
            .thenReturn(Completable.complete());
        when(this.mirrorService.deleteMirror(anyString()))
            .thenReturn(Completable.complete());
    }
}
