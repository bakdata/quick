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

package com.bakdata.quick.mirror.range;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.bakdata.quick.common.TestConfigUtils;
import com.bakdata.quick.common.TestTopicTypeService;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.tags.IntegrationTest;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.mirror.MirrorApplication;
import com.bakdata.quick.mirror.base.HostConfig;
import com.bakdata.quick.mirror.service.context.QueryContextProvider;
import com.bakdata.quick.testutil.AvroRangeQueryTest;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@IntegrationTest
@MicronautTest
@Property(name = "pod.ip", value = "127.0.0.1")
class RangeIndexMirrorIntegrationTest {
    @Inject
    private ObjectMapper objectMapper;
    private static final String INPUT_TOPIC = "range-input";
    @Inject
    HostConfig hostConfig;
    @Inject
    ApplicationContext applicationContext;
    @Inject
    QueryContextProvider queryContextProvider;
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
    void shouldReceiveCorrectValueFromMirrorApplicationWithRangeIndex()
        throws InterruptedException, JsonProcessingException {
        sendValuesToKafka();
        final MirrorApplication<Integer, AvroRangeQueryTest> app = this.setUpApp();
        final Thread runThread = new Thread(app);
        runThread.start();

        Thread.sleep(3000);

        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();

        final MirrorValue<List<GenericRecord>> items = new MirrorValue<>(List.of(avroRecord, avroRecord2, avroRecord3));
        final String expected = this.objectMapper.writeValueAsString(items);

        await()
            .atMost(Duration.ofSeconds(12))
            .untilAsserted(
                () -> RestAssured.given()
                    .queryParam("from", "1")
                    .queryParam("to", "3")
                    .when()
                    .get("http://" + this.hostConfig.toConnectionString() + "/mirror/range/{id}", 1)
                    .then()
                    .statusCode(HttpStatus.OK.getCode())
                    .body(equalTo(expected)));

        app.close();
        app.getStreams().cleanUp();
        runThread.interrupt();
    }

    private static void sendValuesToKafka() throws InterruptedException {
        final AvroRangeQueryTest avroRecord1 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();

        final List<KeyValue<Integer, AvroRangeQueryTest>> keyValueList = List.of(
            new KeyValue<>(1, avroRecord1),
            new KeyValue<>(1, avroRecord2),
            new KeyValue<>(1, avroRecord3));

        final SendKeyValuesTransactional<Integer, AvroRangeQueryTest> sendRequest = SendKeyValuesTransactional
            .inTransaction(INPUT_TOPIC, keyValueList)
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SpecificAvroSerializer.class)
            .with("schema.registry.url", schemaRegistry.getUrl())
            .build();

        kafkaCluster.send(sendRequest);
    }

    private MirrorApplication<Integer, AvroRangeQueryTest> setUpApp() {
        final MirrorApplication<Integer, AvroRangeQueryTest> app = new MirrorApplication<>(
            this.applicationContext, getTopicTypeService(), TestConfigUtils.newQuickTopicConfig(),
            this.hostConfig, this.queryContextProvider
        );
        app.setInputTopics(List.of(INPUT_TOPIC));
        app.setBrokers(kafkaCluster.getBrokerList());
        app.setProductive(false);
        app.setSchemaRegistryUrl(schemaRegistry.getUrl());
        app.setRangeField("timestamp");
        return app;
    }

    private static TopicTypeService getTopicTypeService() {
        return TestTopicTypeService.builder()
            .urlSupplier(schemaRegistry::getUrl)
            .keyType(QuickTopicType.INTEGER)
            .valueType(QuickTopicType.AVRO)
            .keySchema(null)
            .valueSchema(AvroRangeQueryTest.getClassSchema())
            .build();
    }
}
