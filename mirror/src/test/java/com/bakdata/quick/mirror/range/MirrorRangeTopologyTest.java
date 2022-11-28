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

import static com.bakdata.quick.common.TestTypeUtils.newAvroData;
import static com.bakdata.quick.common.TestTypeUtils.newIntegerData;
import static com.bakdata.quick.common.TestTypeUtils.newLongData;
import static com.bakdata.quick.common.TestTypeUtils.newProtobufData;
import static com.bakdata.quick.common.TestTypeUtils.newStringData;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.fluent_kafka_streams_tests.TestTopology;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.schema.SchemaFormat;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.DefaultConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.IndexInputStreamBuilder;
import com.bakdata.quick.mirror.IndexInputStreamBuilder.IndexTopologyData;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.context.RangeIndexProperties;
import com.bakdata.quick.mirror.context.IndexInputStream;
import com.bakdata.quick.mirror.context.RetentionTimeProperties;
import com.bakdata.quick.mirror.range.extractor.AvroExtractor;
import com.bakdata.quick.mirror.range.extractor.ProtoExtractor;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
import com.bakdata.quick.mirror.topology.MirrorTopology;
import com.bakdata.quick.testutil.AvroRangeQueryTest;
import com.bakdata.quick.testutil.ProtoRangeQueryTest;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MirrorRangeTopologyTest {
    private static final List<String> INPUT_TOPICS = List.of("input");
    public static final String MIRROR_STORE = "mirror-test-store";
    private static final String RANGE_STORE_NAME = "range-test-store";
    private static final String RETENTION_STORE = "retention-test-store";
    private static final String RANGE_KEY = "userId";
    private static final String RANGE_FIELD = "timestamp";

    private final SchemaRegistryMock schemaRegistryMock =
        new SchemaRegistryMock(List.of(new AvroSchemaProvider(), new ProtobufSchemaProvider()));

    @AfterEach
    void tearDown() {
        this.schemaRegistryMock.stop();
    }

    @Test
    void shouldWriteInRangeStoreWithAvroSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newIntegerData(), avroData(), new AvroExtractor(), null, SchemaFormat.AVRO),
            setTestProperties(Serdes.Integer(), new GenericAvroSerde())
        );

        testTopology.start();
        final AvroRangeQueryTest avroRecord1 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final AvroRangeQueryTest avroRecord4 = AvroRangeQueryTest.newBuilder().setUserId(2).setTimestamp(1L).build();
        testTopology.input()
            .add(1, avroRecord1)
            .add(1, avroRecord2)
            .add(1, avroRecord3)
            .add(0, avroRecord4)
            .add(2, avroRecord4)
            .add(3, avroRecord4)
            .add(10, avroRecord4)
            .add(-2, avroRecord4)
            .add(-3, avroRecord4)
            .add(-10, avroRecord4);
        final KeyValueStore<String, GenericRecord> store =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        assertThat(store.range("1_0000000000000000001", "1_0000000000000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "1_0000000000000000001",
                        "1_0000000000000000002",
                        "1_0000000000000000003");
                assertThat(keyValues).extracting(keyValue -> keyValue.value.get(RANGE_FIELD))
                    .containsExactly(1L, 2L, 3L);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteNegativeIntegerKeysInRangeStoreWithAvroSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newIntegerData(), avroData(), new AvroExtractor(), null, SchemaFormat.AVRO),
            setTestProperties(Serdes.Integer(), new GenericAvroSerde())
        );
        testTopology.start();

        final AvroRangeQueryTest avroRecord1 = AvroRangeQueryTest.newBuilder().setUserId(-1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(-1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(-1).setTimestamp(3L).build();
        final AvroRangeQueryTest avroRecord4 = AvroRangeQueryTest.newBuilder().setUserId(2).setTimestamp(1L).build();
        testTopology.input()
            .add(-1, avroRecord1)
            .add(-1, avroRecord2)
            .add(-1, avroRecord3)
            .add(0, avroRecord4)
            .add(2, avroRecord4)
            .add(3, avroRecord4)
            .add(10, avroRecord4)
            .add(-2, avroRecord4)
            .add(-3, avroRecord4)
            .add(-10, avroRecord4);

        final KeyValueStore<String, GenericRecord> store =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        assertThat(store.range("-1_0000000000000000001", "-1_0000000000000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "-1_0000000000000000001",
                        "-1_0000000000000000002",
                        "-1_0000000000000000003");
                assertThat(keyValues).extracting(keyValue -> keyValue.value.get(RANGE_FIELD))
                    .containsExactly(1L, 2L, 3L);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteMinAndMaxIntegerKeysInRangeStoreWithAvroSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newIntegerData(), avroData(), new AvroExtractor(), null, SchemaFormat.AVRO),
            setTestProperties(Serdes.Integer(), new GenericAvroSerde())
        );

        testTopology.start();
        final AvroRangeQueryTest avroRecord1 =
            AvroRangeQueryTest.newBuilder().setUserId(Integer.MAX_VALUE).setTimestamp(Long.MIN_VALUE).build();
        final AvroRangeQueryTest avroRecord2 =
            AvroRangeQueryTest.newBuilder().setUserId(Integer.MAX_VALUE).setTimestamp(Long.MAX_VALUE).build();
        final AvroRangeQueryTest avroRecord3 =
            AvroRangeQueryTest.newBuilder().setUserId(Integer.MIN_VALUE).setTimestamp(Long.MIN_VALUE).build();
        final AvroRangeQueryTest avroRecord4 =
            AvroRangeQueryTest.newBuilder().setUserId(Integer.MIN_VALUE).setTimestamp(Long.MAX_VALUE).build();

        testTopology.input()
            .add(Integer.MAX_VALUE, avroRecord1)
            .add(Integer.MAX_VALUE, avroRecord2)
            .add(Integer.MIN_VALUE, avroRecord3)
            .add(Integer.MIN_VALUE, avroRecord4);

        final KeyValueStore<String, GenericRecord> store =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        final String from = String.format("%s_%s", Integer.MAX_VALUE, Long.MIN_VALUE);
        final String to = String.format("%s_%s", Integer.MAX_VALUE, Long.MAX_VALUE);
        final KeyValueIterator<String, GenericRecord> range = store.range(from, to);

        assertThat(range).toIterable()
            .hasSize(2)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key).containsExactly(from, to);

                assertThat(keyValues).extracting(keyValue -> keyValue.value.get(RANGE_FIELD))
                    .containsExactly(Long.MIN_VALUE, Long.MAX_VALUE);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteStringKeysInRangeStoreValueWithAvroSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newStringData(), avroData(), new AvroExtractor(), null, SchemaFormat.AVRO),
            setTestProperties(Serdes.String(), new GenericAvroSerde())
        );

        testTopology.start();
        final AvroRangeQueryTest avroRecord1 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final AvroRangeQueryTest avroRecord4 = AvroRangeQueryTest.newBuilder().setUserId(2).setTimestamp(1L).build();
        testTopology.input()
            .add("abc", avroRecord1)
            .add("abc", avroRecord2)
            .add("abc", avroRecord3)
            .add("fff", avroRecord4)
            .add("eee", avroRecord4)
            .add("rrr", avroRecord4)
            .add("kkk", avroRecord4);
        final KeyValueStore<String, GenericRecord> store =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        assertThat(store.range("abc_0000000000000000001", "abc_0000000000000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "abc_0000000000000000001",
                        "abc_0000000000000000002",
                        "abc_0000000000000000003");
                assertThat(keyValues).extracting(keyValue -> keyValue.value.get(RANGE_FIELD))
                    .containsExactly(1L, 2L, 3L);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteInRangeStoreWithProtoSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newLongData(), protoData(), new ProtoExtractor(), null, SchemaFormat.PROTOBUF),
            setTestProperties(Serdes.Long(), new KafkaProtobufSerde<>())
        ).withSchemaRegistryMock(new SchemaRegistryMock(List.of(new ProtobufSchemaProvider())));

        testTopology.start();

        final ProtoRangeQueryTest protoRecord1 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(1).build();
        final ProtoRangeQueryTest protoRecord2 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(2).build();
        final ProtoRangeQueryTest protoRecord3 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(3).build();
        final ProtoRangeQueryTest protoRecord4 = ProtoRangeQueryTest.newBuilder().setUserId(2L).setTimestamp(1).build();
        testTopology.input()
            .add(1L, protoRecord1)
            .add(1L, protoRecord2)
            .add(1L, protoRecord3)
            .add(0L, protoRecord4)
            .add(2L, protoRecord4)
            .add(3L, protoRecord4)
            .add(10L, protoRecord4)
            .add(-2L, protoRecord4)
            .add(-3L, protoRecord1)
            .add(-3L, protoRecord2)
            .add(-3L, protoRecord3)
            .add(-3L, protoRecord4)
            .add(-10L, protoRecord4);
        final KeyValueStore<String, Message> store = testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);

        assertThat(store.range("1_0000000001", "1_0000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "1_0000000001",
                        "1_0000000002",
                        "1_0000000003");
                assertThat(keyValues).extracting(keyValue -> getField(keyValue.value))
                    .containsExactly(1, 2, 3);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteNegativeKeysInRangeStoreWithProtoSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newLongData(), protoData(), new ProtoExtractor(), null, SchemaFormat.PROTOBUF),
            setTestProperties(Serdes.Long(), new KafkaProtobufSerde<>())
        ).withSchemaRegistryMock(new SchemaRegistryMock(List.of(new ProtobufSchemaProvider())));

        testTopology.start();

        final ProtoRangeQueryTest protoRecord1 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(1).build();
        final ProtoRangeQueryTest protoRecord2 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(2).build();
        final ProtoRangeQueryTest protoRecord3 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(3).build();
        final ProtoRangeQueryTest protoRecord4 = ProtoRangeQueryTest.newBuilder().setUserId(2L).setTimestamp(1).build();

        testTopology.input()
            .add(-1L, protoRecord1)
            .add(-1L, protoRecord2)
            .add(-1L, protoRecord3)
            .add(0L, protoRecord4)
            .add(2L, protoRecord4)
            .add(3L, protoRecord4)
            .add(10L, protoRecord4)
            .add(-2L, protoRecord4)
            .add(-3L, protoRecord4)
            .add(-10L, protoRecord4)
        ;
        final KeyValueStore<String, Message> store = testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        assertThat(store.range("-1_0000000001", "-1_0000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "-1_0000000001",
                        "-1_0000000002",
                        "-1_0000000003");
                assertThat(keyValues).extracting(keyValue -> getField(keyValue.value))
                    .containsExactly(1, 2, 3);
            });

        testTopology.stop();
    }

    @Test
    void shouldWriteMinAndMaxLongKeysInRangeStoreWithProtoSchemaValue() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newLongData(), protoData(), new ProtoExtractor(), null, SchemaFormat.PROTOBUF),
            setTestProperties(Serdes.Long(), new KafkaProtobufSerde<>())
        ).withSchemaRegistryMock(new SchemaRegistryMock(List.of(new ProtobufSchemaProvider())));

        testTopology.start();

        final ProtoRangeQueryTest protoRecord1 =
            ProtoRangeQueryTest.newBuilder().setUserId(Long.MIN_VALUE).setTimestamp(Integer.MIN_VALUE).build();
        final ProtoRangeQueryTest protoRecord2 =
            ProtoRangeQueryTest.newBuilder().setUserId(Long.MIN_VALUE).setTimestamp(Integer.MAX_VALUE).build();
        final ProtoRangeQueryTest protoRecord3 =
            ProtoRangeQueryTest.newBuilder().setUserId(Long.MAX_VALUE).setTimestamp(Integer.MIN_VALUE).build();
        final ProtoRangeQueryTest protoRecord4 =
            ProtoRangeQueryTest.newBuilder().setUserId(Long.MAX_VALUE).setTimestamp(Integer.MAX_VALUE).build();

        testTopology.input()
            .add(Long.MIN_VALUE, protoRecord1)
            .add(Long.MIN_VALUE, protoRecord2)
            .add(Long.MAX_VALUE, protoRecord3)
            .add(Long.MAX_VALUE, protoRecord4);
        final KeyValueStore<String, Message> store = testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);

        final String from = String.format("%s_%s", Long.MIN_VALUE, Integer.MIN_VALUE);
        final String to = String.format("%s_%s", Long.MIN_VALUE, Integer.MAX_VALUE);
        final KeyValueIterator<String, Message> range = store.range(from, to);
        assertThat(range).toIterable()
            .hasSize(2)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(from, to);
                assertThat(keyValues).extracting(keyValue -> getField(keyValue.value))
                    .containsExactly(Integer.MIN_VALUE, Integer.MAX_VALUE);
            });
        testTopology.stop();
    }

    @Test
    void shouldWriteToPointStoreAndRangeStoreWithAvroSchemaWhenRangeKeyIsSet() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newStringData(), avroData(), new AvroExtractor(), RANGE_KEY, SchemaFormat.AVRO),
            setTestProperties(Serdes.String(), new GenericAvroSerde())
        );

        testTopology.start();
        final AvroRangeQueryTest avroRecord1 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final AvroRangeQueryTest avroRecord2 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(2L).build();
        final AvroRangeQueryTest avroRecord3 = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final AvroRangeQueryTest avroRecord4 = AvroRangeQueryTest.newBuilder().setUserId(2).setTimestamp(1L).build();
        testTopology.input()
            .add("abc", avroRecord1)
            .add("abc", avroRecord2)
            .add("abc", avroRecord3)
            .add("fff", avroRecord4)
            .add("eee", avroRecord4)
            .add("rrr", avroRecord4)
            .add("kkk", avroRecord4);

        final KeyValueStore<String, GenericRecord> store =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);
        assertThat(store.range("1_0000000000000000001", "1_0000000000000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "1_0000000000000000001",
                        "1_0000000000000000002",
                        "1_0000000000000000003");
                assertThat(keyValues).extracting(keyValue -> keyValue.value.get(RANGE_FIELD))
                    .containsExactly(1L, 2L, 3L);
            });

        final KeyValueStore<Integer, GenericRecord> pointStore =
            testTopology.getTestDriver().getKeyValueStore(MIRROR_STORE);
        final GenericRecord expected = pointStore.get(1);
        assertThat(avroRecord3.get("userId")).isEqualTo(expected.get("userId"));
        assertThat(avroRecord3.get("timestamp")).isEqualTo(expected.get("timestamp"));
        testTopology.stop();
    }

    @Test
    void shouldToWritePointStoreAndRangeStoreWithProtoSchemaWhenRangeKeyIsSet() {
        final TestTopology<Object, Object> testTopology = new TestTopology<>(properties ->
            createTopology(properties, newStringData(), protoData(), new ProtoExtractor(), RANGE_KEY,
                SchemaFormat.PROTOBUF),
            setTestProperties(Serdes.String(), new KafkaProtobufSerde<>())
        ).withSchemaRegistryMock(new SchemaRegistryMock(List.of(new ProtobufSchemaProvider())));

        testTopology.start();

        final ProtoRangeQueryTest protoRecord1 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(1).build();
        final ProtoRangeQueryTest protoRecord2 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(2).build();
        final ProtoRangeQueryTest protoRecord3 = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(3).build();
        final ProtoRangeQueryTest protoRecord4 = ProtoRangeQueryTest.newBuilder().setUserId(2L).setTimestamp(1).build();
        testTopology.input()
            .add("abc", protoRecord1)
            .add("abc", protoRecord2)
            .add("abc", protoRecord3)
            .add("fff", protoRecord4)
            .add("eee", protoRecord4)
            .add("rrr", protoRecord4)
            .add("kkk", protoRecord4);
        final KeyValueStore<String, Message> rangeStore =
            testTopology.getTestDriver().getKeyValueStore(RANGE_STORE_NAME);

        assertThat(rangeStore.range("1_0000000001", "1_0000000003")).toIterable()
            .hasSize(3)
            .satisfies(keyValues -> {
                assertThat(keyValues).extracting(keyValue -> keyValue.key)
                    .containsExactly(
                        "1_0000000001",
                        "1_0000000002",
                        "1_0000000003");
                assertThat(keyValues).extracting(keyValue -> getField(keyValue.value))
                    .containsExactly(1, 2, 3);
            });

        final KeyValueStore<Long, Message> pointStore =
            testTopology.getTestDriver().getKeyValueStore(MIRROR_STORE);
        final Message actual = pointStore.get(1L);
        assertThat(actual.toByteArray()).isEqualTo(protoRecord3.toByteArray());
        testTopology.stop();
    }

    private static <K, V> Topology createTopology(final Properties properties, final QuickData<K> quickKeyData,
        final QuickData<V> quickValueData, final SchemaExtractor schemaExtractor, @Nullable final String rangeKey,
        final SchemaFormat schemaFormat) {

        final String topic = INPUT_TOPICS.get(0);

        final QuickTopicData<K, V> data =
            new QuickTopicData<>(topic, TopicWriteType.MUTABLE, quickKeyData, quickValueData);
        data.getKeyData().getSerde().configure(Maps.fromProperties(properties), true);
        data.getValueData().getSerde().configure(Maps.fromProperties(properties), false);

        final QuickTopologyData<K, V> topologyInfo =
            QuickTopologyData.<K, V>builder()
                .inputTopics(INPUT_TOPICS)
                .topicData(data)
                .build();

        final SchemaConfig schemaConfig = new SchemaConfig(Optional.of(schemaFormat), Optional.empty());
        final KafkaConfig kafkaConfig = new KafkaConfig("", "");
        final ConversionProvider conversionProvider = new DefaultConversionProvider(kafkaConfig, schemaConfig);

        final IndexInputStreamBuilder
            indexInputStreamBuilder = new IndexInputStreamBuilder(schemaExtractor, conversionProvider);

        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final IndexTopologyData<K, V>
            indexTopologyData = indexInputStreamBuilder.consume(topologyInfo, streamsBuilder, rangeKey);
        final IndexInputStream<K, V> indexInputStream = indexTopologyData.getIndexInputStream();

        final MirrorContext<K, V> mirrorContext = MirrorContext.<K, V>builder()
            .streamsBuilder(streamsBuilder)
            .pointStoreName(MIRROR_STORE)
            .indexInputStream(indexInputStream)
            .rangeIndexProperties(new RangeIndexProperties(RANGE_STORE_NAME, RANGE_FIELD))
            .rangeKey(rangeKey)
            .storeType(StoreType.INMEMORY)
            .retentionTimeProperties(new RetentionTimeProperties(RETENTION_STORE, null))
            .schemaExtractor(schemaExtractor)
            .build();

        return new MirrorTopology<>(mirrorContext).createTopology(indexTopologyData.getStream());
    }

    private static QuickData<GenericRecord> avroData() {
        return newAvroData(AvroRangeQueryTest.getClassSchema());
    }

    private static QuickData<Message> protoData() {
        return newProtobufData(ProtoRangeQueryTest.getDescriptor());
    }

    private static <K, V> Map<String, String> setTestProperties(final Serde<K> keySerde, final Serde<V> valueSerde) {
        return Map.of("bootstrap.servers", "test:123", "application.id", "mirror-test",
            StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerde.getClass().getName(),
            StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde.getClass().getName());
    }

    private static Object getField(final MessageOrBuilder keyValue) {
        final FieldDescriptor fieldDescriptor = keyValue.getDescriptorForType().findFieldByName(RANGE_FIELD);
        return keyValue.getField(fieldDescriptor);
    }
}
