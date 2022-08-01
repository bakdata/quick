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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class KafkaIngestServiceTest {
    private static final String TOPIC = "topic";
    private final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();
    private KafkaIngestService service = null;
    private EmbeddedKafkaCluster kafkaCluster = null;
    @Inject
    private TopicTypeService typeService;

    @BeforeEach
    void setUp() {
        this.schemaRegistry.start();
        this.kafkaCluster = provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
        this.kafkaCluster.start();
        this.kafkaCluster.createTopic(TopicConfig.withName(TOPIC).useDefaults());
        final KafkaConfig kafkaConfig =
            new KafkaConfig(this.kafkaCluster.getBrokerList(), this.schemaRegistry.getUrl());
        this.service = new KafkaIngestService(this.typeService, kafkaConfig);
    }

    @AfterEach
    void tearDown() {
        this.kafkaCluster.stop();
        this.schemaRegistry.stop();
    }

    @Test
    void testSendData() throws InterruptedException {
        final KeyValuePair<String, Long> record = new KeyValuePair<>("foo", 5L);

        final Throwable throwable = this.service.sendData(TOPIC, List.of(record)).blockingGet();
        if (throwable != null) {
            fail(throwable.getMessage());
        }

        assertThat(this.kafkaCluster.read(ReadKeyValues.from(TOPIC, String.class, Long.class)
            .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
            .build()))
            .contains(new KeyValue<>("foo", 5L));
    }

    @Test
    void testDeleteData() throws InterruptedException {
        final KeyValue<String, Long> record = new KeyValue<>("foo", 5L);

        final SendKeyValuesTransactional<String, Long> transactionalBuilder =
            SendKeyValuesTransactional.inTransaction(TOPIC, List.of(record))
                .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
                .build();
        this.kafkaCluster.send(transactionalBuilder);
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                final List<KeyValue<String, Long>> keyValues =
                    this.kafkaCluster.read(ReadKeyValues.from(TOPIC, String.class, Long.class)
                        .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                        .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
                        .build());

                assertThat(keyValues).containsExactly(new KeyValue<>("foo", 5L));
            });

        final Throwable throwable = this.service.deleteData(TOPIC, List.of(record.getKey())).blockingGet();
        if (throwable != null) {
            fail(throwable.getMessage());
        }

        // writing null into the topic deletes the value from the store - for now that is good enough
        assertThat(this.kafkaCluster.read(ReadKeyValues.from(TOPIC, String.class, Long.class)
            .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
            .build()))
            .containsExactly(new KeyValue<>("foo", 5L), new KeyValue<>("foo", null));
    }

    @MockBean(TopicTypeService.class)
    TopicTypeService topicTypeService() {
        return TestTopicTypeService.builder()
            .urlSupplier(this.schemaRegistry::getUrl)
            .keyType(QuickTopicType.STRING)
            .valueType(QuickTopicType.LONG)
            .build();
    }
}
