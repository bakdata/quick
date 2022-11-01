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

package com.bakdata.quick.mirror.point;

import static io.restassured.RestAssured.when;
import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.bakdata.quick.common.TestConfigUtils;
import com.bakdata.quick.common.TestTopicTypeService;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.schema.SchemaFormat;
import com.bakdata.quick.common.tags.IntegrationTest;
import com.bakdata.quick.common.type.DefaultConversionProvider;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.mirror.MirrorApplication;
import com.bakdata.quick.mirror.StreamConsumer;
import com.bakdata.quick.mirror.base.HostConfig;
import com.bakdata.quick.mirror.context.MirrorContextProvider;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@IntegrationTest
@MicronautTest
@Property(name = "pod.ip", value = "127.0.0.1")
class PointIndexStreamsStateIntegrationTest {
    public static final String INPUT_TOPIC = "input";
    @Inject
    HostConfig hostConfig;
    @Inject
    ApplicationContext applicationContext;
    @Inject
    MirrorContextProvider<String, String> mirrorContextProvider;
    @Inject
    SchemaExtractor schemaExtractor;

    private static final EmbeddedKafkaCluster kafkaCluster =
        provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
    private static final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();

    @BeforeAll
    static void setup() {
        schemaRegistry.start();
        kafkaCluster.start();
    }

    @AfterAll
    static void teardown() {
        schemaRegistry.stop();
        kafkaCluster.stop();
    }

    @Test
    void shouldReceiveCorrectPartitionHostFromMirrorApplication() throws InterruptedException {
        sendValuesToKafka();
        final MirrorApplication<String, String, String> app = this.setUpApp();
        final Thread runThread = new Thread(app);
        runThread.start();

        final int port = this.hostConfig.getPort();
        final String expectedBody = String.format("{\"0\":\"127.0.0.1:%d\"}", port);
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> when()
                .get("http://" + this.hostConfig.toConnectionString() + "/streams/partitions")
                .then()
                .statusCode(HttpStatus.OK.getCode())
                .body(equalTo(expectedBody)));
        app.close();
        app.getStreams().cleanUp();
        runThread.interrupt();
    }


    private static void sendValuesToKafka() throws InterruptedException {
        final List<KeyValue<String, String>> keyValueList = List.of(new KeyValue<>("key1", "value1"),
            new KeyValue<>("key2", "value2"),
            new KeyValue<>("key3", "value3"));
        final SendKeyValuesTransactional<String, String> sendRequest = SendKeyValuesTransactional
            .inTransaction(INPUT_TOPIC, keyValueList)
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .with("schema.registry.url", schemaRegistry.getUrl())
            .build();

        kafkaCluster.send(sendRequest);
    }

    private static TopicTypeService topicTypeService() {
        return TestTopicTypeService.builder()
            .urlSupplier(schemaRegistry::getUrl)
            .keyType(QuickTopicType.STRING)
            .valueType(QuickTopicType.STRING)
            .keySchema(null)
            .valueSchema(null)
            .build();
    }

    private MirrorApplication<String, String, String> setUpApp() {
        final KafkaConfig kafkaConfig = new KafkaConfig("dummy:123", schemaRegistry.getUrl());
        final SchemaConfig schemaConfig = new SchemaConfig(Optional.of(SchemaFormat.AVRO), Optional.empty());
        final DefaultConversionProvider defaultConversionProvider =
            new DefaultConversionProvider(kafkaConfig, schemaConfig);

        final MirrorApplication<String, String, String> app = new MirrorApplication<>(
            this.schemaExtractor,
            this.applicationContext,
            topicTypeService(),
            TestConfigUtils.newQuickTopicConfig(),
            this.hostConfig,
            this.mirrorContextProvider,
            new StreamConsumer(this.schemaExtractor, defaultConversionProvider)
        );
        app.setInputTopics(List.of(INPUT_TOPIC));
        app.setBrokers(kafkaCluster.getBrokerList());
        app.setProductive(false);
        app.setSchemaRegistryUrl(schemaRegistry.getUrl());
        return app;
    }
}
