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

package com.bakdata.quick.common.type.inferer;


import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.useDefaults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.schema.SchemaFetcher;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import io.reactivex.Single;
import java.util.List;
import java.util.Random;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class InferTopicTypeServiceTest {
    public static final String INPUT_TOPIC = "input";
    private static final String TEST_ID = "testString";
    private static final EmbeddedKafkaCluster kafkaCluster = provisionWith(useDefaults());
    private static final SchemaFetcher schemaFetcher = mock(SchemaFetcher.class);
    @RegisterExtension
    final SchemaRegistryMockExtension schemaRegistryMock = new SchemaRegistryMockExtension();
    private InferTopicTypeService service;

    @BeforeAll
    static void beforeAll() {
        kafkaCluster.start();
    }

    @AfterAll
    static void afterAll() {
        kafkaCluster.stop();
    }

    private static ChartRecord newRecord() {
        final Random random = new Random();
        return ChartRecord.newBuilder().setFieldId(random.nextLong()).setCountPlays(random.nextLong()).build();
    }

    @BeforeEach
    void setUp() {
        this.service = new InferTopicTypeService(kafkaCluster.getBrokerList(), TEST_ID, schemaFetcher);
    }

    @Test
    void testString() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-string";
        final SendKeyValuesTransactional<String, String> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>("key1", "value1"),
                new KeyValue<>("key2222", "value2"),
                new KeyValue<>("key2", "value2")))
            .useDefaults();
        kafkaCluster.send(sendRequest);
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.STRING);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.STRING);
    }

    @Test
    void testInt() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-int";
        final SendKeyValuesTransactional<Integer, Integer> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>(1, 2),
                new KeyValue<>(3, 4),
                new KeyValue<>(5, 6)))
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .build();

        kafkaCluster.send(sendRequest);
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.INTEGER);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.INTEGER);
    }

    @Test
    void testLong() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-long";
        final SendKeyValuesTransactional<Long, Long> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>(1L, 2L),
                new KeyValue<>(3L, 4L),
                new KeyValue<>(5L, 6L)))
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
            .build();

        kafkaCluster.send(sendRequest);
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.LONG);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.LONG);
    }

    @Test
    void testMixed() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-mixed";
        final SendKeyValuesTransactional<Integer, String> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>(1, "value1"),
                new KeyValue<>(2, "value2"),
                new KeyValue<>(3, "value2")))
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .build();

        kafkaCluster.send(sendRequest);
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.INTEGER);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.STRING);
    }

    @Test
    void testValueAvro() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-valueAvro";
        this.schemaRegistryMock.registerValueSchema(topic, ChartRecord.getClassSchema());
        final SendKeyValuesTransactional<String, ChartRecord> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>("key1", newRecord()),
                new KeyValue<>("key2222", newRecord()),
                new KeyValue<>("key2", newRecord())))
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GenericAvroSerializer.class)
            .with(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryMock.getUrl())
            .build();

        kafkaCluster.send(sendRequest);
        when(schemaFetcher.getValueSchema(topic)).thenReturn(Single.just(ChartRecord.getClassSchema()));
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.STRING);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.SCHEMA);
    }

    @Test
    void testKeyAvro() throws InterruptedException {
        final String topic = INPUT_TOPIC + "-keyAvro";
        this.schemaRegistryMock.registerValueSchema(topic, ChartRecord.getClassSchema());
        final SendKeyValuesTransactional<ChartRecord, Long> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, List.of(new KeyValue<>(newRecord(), 2L),
                new KeyValue<>(newRecord(), 4L),
                new KeyValue<>(newRecord(), 6L)))
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, GenericAvroSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, LongSerializer.class)
            .with(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryMock.getUrl())
            .build();

        kafkaCluster.send(sendRequest);
        when(schemaFetcher.getKeySchema(topic)).thenReturn(Single.just(ChartRecord.getClassSchema()));
        final QuickTopicData<?, ?> types = this.service.getTopicData(topic).blockingGet();

        assertThat(types.getKeyData().getType()).isEqualTo(QuickTopicType.SCHEMA);
        assertThat(types.getValueData().getType()).isEqualTo(QuickTopicType.LONG);
    }

}
