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

package com.bakdata.quick.ingest.service;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import com.bakdata.quick.common.TestTopicTypeService;
import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ReadKeyValues;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import net.mguenther.kafka.junit.TopicConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@MicronautTest
class KafkaIngestServiceTest {
    private static final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();
    private static EmbeddedKafkaCluster kafkaCluster = null;

    @Inject
    private TopicTypeService typeService;

    @BeforeAll
    static void setUp() {
        kafkaCluster = provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
        kafkaCluster.start();
        schemaRegistry.start();
    }

    @AfterAll
    static void tearDown() {
        schemaRegistry.stop();
        kafkaCluster.stop();
    }

    @Test
    void testSendData(final TestInfo testInfo) throws InterruptedException {
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), schemaRegistry.getUrl());
        final KafkaIngestService kafkaIngestService = new KafkaIngestService(this.typeService, kafkaConfig);

        final String topic = String.format("%s-topic", testInfo.getTestMethod().get().getName());
        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());

        final KeyValuePair<String, Long> record = new KeyValuePair<>("foo", 5L);

        final Throwable throwable = kafkaIngestService.sendData(topic, List.of(record)).blockingGet();
        if (throwable != null) {
            fail(throwable.getMessage());
        }

        assertThat(kafkaCluster.read(ReadKeyValues.from(topic, String.class, Long.class)
            .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
            .build()))
            .contains(new KeyValue<>("foo", 5L));
    }

    @Test
    void testDeleteData(final TestInfo testInfo) throws InterruptedException {
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), schemaRegistry.getUrl());
        final KafkaIngestService kafkaIngestService = new KafkaIngestService(this.typeService, kafkaConfig);

        final String topic = String.format("%s-topic", testInfo.getTestMethod().get().getName());
        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());
        final KeyValue<String, Long> record = new KeyValue<>("foo", 5L);

        final SendKeyValuesTransactional<String, Long> transactionalBuilder =
            SendKeyValuesTransactional.inTransaction(topic, List.of(record))
                .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
                .build();
        kafkaCluster.send(transactionalBuilder);
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                final List<KeyValue<String, Long>> keyValues =
                    kafkaCluster.read(ReadKeyValues.from(topic, String.class, Long.class)
                        .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                        .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
                        .build());

                assertThat(keyValues).containsExactly(new KeyValue<>("foo", 5L));
            });

        final Throwable throwable = kafkaIngestService.deleteData(topic, List.of(record.getKey())).blockingGet();
        if (throwable != null) {
            fail(throwable.getMessage());
        }

        // writing null into the topic deletes the value from the store - for now that is good enough
        assertThat(kafkaCluster.read(ReadKeyValues.from(topic, String.class, Long.class)
            .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
            .build()))
            .containsExactly(new KeyValue<>("foo", 5L), new KeyValue<>("foo", null));
    }

    @MockBean(TopicTypeService.class)
    TopicTypeService topicTypeService() {
        return TestTopicTypeService.builder()
            .urlSupplier(schemaRegistry::getUrl)
            .keyType(QuickTopicType.STRING)
            .valueType(QuickTopicType.LONG)
            .build();
    }
}
