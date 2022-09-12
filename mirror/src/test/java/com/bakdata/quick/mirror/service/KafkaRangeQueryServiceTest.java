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

package com.bakdata.quick.mirror.service;

import static com.bakdata.quick.common.TestTypeUtils.newAvroData;
import static com.bakdata.quick.common.TestTypeUtils.newIntegerData;
import static org.junit.jupiter.api.Assertions.*;

import com.bakdata.fluent_kafka_streams_tests.TestTopology;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.mirror.MirrorTopology;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.testutil.AvroRangeQueryTest;
import com.bakdata.quick.testutil.ProtoRangeQueryTest;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import com.google.common.collect.Maps;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class KafkaRangeQueryServiceTest {
    private static final List<String> INPUT_TOPICS = List.of("input");
    private static final String RANGE_STORE_NAME = "range-test-store";
    private static final String RANGE_FIELD = "timestamp";

    @Inject
    QueryContextProvider queryContextProvider;

    private final SchemaRegistryMock schemaRegistryMock =
        new SchemaRegistryMock(List.of(new AvroSchemaProvider(), new ProtobufSchemaProvider()));

    @AfterEach
    void tearDown() {
        this.schemaRegistryMock.stop();
    }

    @Test
    void shouldAddRangeValueWithAvroSchema() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(
            KafkaRangeQueryServiceTest::createAvroTopology, avroTestProps()
        );

        testTopology.start();

        final ProtoRangeQueryTest protoRecord1 = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1).build();
        final ProtoRangeQueryTest protoRecord2 = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2).build();
        final ProtoRangeQueryTest protoRecord3 = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3).build();
        final ProtoRangeQueryTest protoRecord4 = ProtoRangeQueryTest.newBuilder().setUserId(2).setTimestamp(1).build();
        testTopology.input()
            .add(1, protoRecord1)
            .add(1, protoRecord2)
            .add(1, protoRecord3)
            .add(0, protoRecord4)
            .add(2, protoRecord4)
            .add(3, protoRecord4)
            .add(10, protoRecord4)
            .add(-2, protoRecord4)
            .add(-3, protoRecord4)
            .add(-10, protoRecord4);
    }

    private static Topology createAvroTopology(final Properties properties) {
        final String topic = INPUT_TOPICS.get(0);
        final QuickTopicData<Integer, GenericRecord> data =
            new QuickTopicData<>(topic, TopicWriteType.MUTABLE, newIntegerData(), avroData());
        data.getKeyData().getSerde().configure(Maps.fromProperties(properties), true);
        data.getValueData().getSerde().configure(Maps.fromProperties(properties), false);

        final QuickTopologyData<Integer, GenericRecord> topologyInfo =
            QuickTopologyData.<Integer, GenericRecord>builder()
                .inputTopics(INPUT_TOPICS)
                .topicData(data)
                .build();

        final MirrorTopology<Integer, GenericRecord> mirrorTopology = MirrorTopology.<Integer, GenericRecord>builder()
            .topologyData(topologyInfo)
            .rangeStoreName(RANGE_STORE_NAME)
            .isPoint(false)
            .rangeField(RANGE_FIELD)
            .storeType(StoreType.INMEMORY)
            .build();

        final StreamsBuilder builder = new StreamsBuilder();
        return mirrorTopology.createTopology(builder);
    }

    private static Map<String, String> avroTestProps() {
        return Map.of("bootstrap.servers", "test:123", "application.id", "mirror-test",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName(),
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());
    }

    private static QuickData<GenericRecord> avroData() {
        return newAvroData(AvroRangeQueryTest.getClassSchema());
    }


}