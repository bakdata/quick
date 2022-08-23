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

package com.bakdata.quick.mirror;

import static io.restassured.RestAssured.when;
import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.bakdata.quick.common.TestConfigUtils;
import com.bakdata.quick.common.TestTopicTypeService;
import com.bakdata.quick.common.tags.IntegrationTest;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.mirror.base.HostConfig;
import com.bakdata.quick.mirror.service.QueryContextProvider;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
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
class MirrorApplicationIntegrationTest {
    public static final String INPUT_TOPIC = "input";
    @Inject
    HostConfig hostConfig;
    @Inject
    ApplicationContext applicationContext;
    @Inject
    QueryContextProvider queryContextProvider;
    private static EmbeddedKafkaCluster kafkaCluster;
    private static final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();

    @BeforeAll
    static void setup() {
        kafkaCluster = provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
        schemaRegistry.start();
        kafkaCluster.start();
    }

    @AfterAll
    static void teardown() {
        schemaRegistry.stop();
        kafkaCluster.stop();
    }

    @Test
    void shouldReceiveCorrectValueFromMirrorApplication() throws InterruptedException {
        sendValuesToKafka();
        final MirrorApplication<String, String> app = this.setUpApp();
        final Thread runThread = new Thread(app);
        runThread.start();

        Thread.sleep(3000);

        await()
                .atMost(Duration.ofSeconds(12))
                .untilAsserted(
                        () -> when().get("http://" + this.hostConfig.toConnectionString() + "/mirror/{id}", "key1")
                                .then()
                                .statusCode(200)
                                .body(equalTo("{\"value\":\"value1\"}")));
        app.close();
        app.getStreams().cleanUp();
        runThread.interrupt();
    }

    private void sendValuesToKafka() throws InterruptedException {
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

    private MirrorApplication<String, String> setUpApp() {
        final MirrorApplication<String, String> app = new MirrorApplication<>(
                this.applicationContext, this.topicTypeService(), TestConfigUtils.newQuickTopicConfig(),
                this.hostConfig, this.queryContextProvider
        );
        app.setInputTopics(List.of(INPUT_TOPIC));
        app.setBrokers(kafkaCluster.getBrokerList());
        app.setProductive(false);
        app.setSchemaRegistryUrl(schemaRegistry.getUrl());
        return app;
    }

    private TopicTypeService topicTypeService() {
        return TestTopicTypeService.builder()
                .urlSupplier(schemaRegistry::getUrl)
                .keyType(QuickTopicType.STRING)
                .valueType(QuickTopicType.STRING)
                .keySchema(null)
                .valueSchema(null)
                .build();
    }
}
