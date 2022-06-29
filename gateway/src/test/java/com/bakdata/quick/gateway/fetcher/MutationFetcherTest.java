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

package com.bakdata.quick.gateway.fetcher;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.TestTopicTypeService;
import com.bakdata.quick.common.TestTypeUtils;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.json.AvroJacksonConfiguration;
import com.bakdata.quick.common.tags.IntegrationTest;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.ingest.KafkaIngestService;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ReadKeyValues;
import net.mguenther.kafka.junit.TopicConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@IntegrationTest
class MutationFetcherTest {
    private static EmbeddedKafkaCluster kafkaCluster = null;
    private static final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setup() {
        schemaRegistry.start();
        kafkaCluster = provisionWith(defaultClusterConfig());
        kafkaCluster.start();
        new AvroJacksonConfiguration().configureObjectMapper(objectMapper);
    }

    @AfterAll
    static void afterAll() {
        kafkaCluster.stop();
        schemaRegistry.stop();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideValueArguments() {
        return Stream.of(
            // integer
            Arguments.of(
                TestParameterBuilder.<String, Integer, Integer>builder()
                    .topic("integer-test-values-test")
                    .keyValue(keyValuePairsWithRandomKey(2))
                    .infoType(TestTypeUtils.newIntegerData())
                    .classType(Integer.class)
                    .build()
            ),
            // long
            Arguments.of(
                TestParameterBuilder.<String, Long, Long>builder()
                    .topic("long-test-values-test")
                    .keyValue(keyValuePairsWithRandomKey(2L))
                    .infoType(TestTypeUtils.newLongData())
                    .classType(Long.class)
                    .build()
            ),
            // double
            Arguments.of(
                TestParameterBuilder.<String, Double, Double>builder()
                    .topic("double-test-values-test")
                    .keyValue(keyValuePairsWithRandomKey(2.))
                    .infoType(TestTypeUtils.newDoubleData())
                    .classType(Double.class)
                    .build()
            ),
            // string
            Arguments.of(
                TestParameterBuilder.<String, String, String>builder()
                    .topic("string-test-values-test")
                    .keyValue(keyValuePairsWithRandomKey("2"))
                    .infoType(TestTypeUtils.newStringData())
                    .classType(String.class)
                    .build()
            ),
            // schema
            Arguments.of(
                TestParameterBuilder.<String, GenericRecord, GenericRecord>builder()
                    .topic("schema-test-value-test")
                    .keyValue(new KeyValue<>(RandomStringUtils.random(1), inputRecord()))
                    .infoType(TestTypeUtils.newAvroData(ChartRecord.getClassSchema()))
                    .build()
            )
        );
    }

    private static GenericRecord inputRecord() {
        return new GenericRecordBuilder(ChartRecord.getClassSchema()).set("fieldId", 5L).set("countPlays", 5L).build();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideValueArgumentsForKeys() {
        return Stream.of(
            // integer
            Arguments.of(
                TestParameterBuilder.<Integer, String, Integer>builder()
                    .topic("integer-test-key-test")
                    .keyValue(keyValuePairsWithRandomValue(2))
                    .infoType(TestTypeUtils.newIntegerData())
                    .build()
            ),
            // long
            Arguments.of(
                TestParameterBuilder.<Long, String, Long>builder()
                    .topic("long-test-key-test")
                    .keyValue(keyValuePairsWithRandomValue(2L))
                    .infoType(TestTypeUtils.newLongData())
                    .build()
            ),
            // double
            Arguments.of(
                TestParameterBuilder.<Double, String, Double>builder()
                    .topic("double-test-key-test")
                    .keyValue(keyValuePairsWithRandomValue(2.))
                    .infoType(TestTypeUtils.newDoubleData())
                    .build()
            ),
            // string
            Arguments.of(
                TestParameterBuilder.<String, String, String>builder()
                    .topic("string-test-key-test")
                    .keyValue(keyValuePairsWithRandomValue("2"))
                    .infoType(TestTypeUtils.newStringData())
                    .build()
            )
        );
    }

    private static <V> KeyValue<String, V> keyValuePairsWithRandomKey(final V value) {
        return new KeyValue<>(RandomStringUtils.random(1), value);
    }


    private static <K> KeyValue<K, String> keyValuePairsWithRandomValue(final K value) {
        return new KeyValue<>(value, RandomStringUtils.random(1));
    }

    @ParameterizedTest(name = "shouldIngestDataWithDifferentValueTypes ({0})")
    @MethodSource("provideValueArguments")
    <V, T> void shouldIngestDataWithDifferentValueTypes(final TestParameterBuilder<String, V, T> testParameter)
        throws Exception {

        final String topic = testParameter.getTopic();
        final QuickData<T> valueInfo = testParameter.getInfoType();

        final QuickTopicData<String, T> info = new QuickTopicData<>(
            topic,
            TopicWriteType.MUTABLE,
            TestTypeUtils.newStringData(),
            valueInfo
        );
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), schemaRegistry.getUrl());

        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());

        final KafkaIngestService kafkaIngestService =
            new KafkaIngestService(
                topicTypeService(QuickTopicType.STRING, valueInfo.getType(), ChartRecord.getClassSchema()),
                kafkaConfig);

        final DataFetcher<T> mutationFetcher =
            new MutationFetcher<>(topic,
                "id",
                "name", new Lazy<>(() -> info),
                kafkaIngestService,
                objectMapper
            );

        final KeyValue<String, V> keyValue = testParameter.getKeyValue();
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", keyValue.getKey(), "name", keyValue.getValue()))
            .build();

        final T actual = mutationFetcher.get(env);

        assertThat(actual).isEqualTo(keyValue.getValue());

        final Optional<KeyValue<String, T>> consumedRecords =
            kafkaCluster.read(ReadKeyValues.from(topic, testParameter.getClassType())
                    .with("schema.registry.url", schemaRegistry.getUrl())
                    .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        valueInfo.getSerde().deserializer().getClass()))
                .stream().findFirst();

        assertThat(consumedRecords)
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("key", keyValue.getKey())
            .hasFieldOrPropertyWithValue("value", keyValue.getValue());

    }

    @ParameterizedTest(name = "shouldIngestDataWithDifferentKeyTypes ({0})")
    @MethodSource("provideValueArgumentsForKeys")
    <K, T> void shouldIngestDataWithDifferentKeyTypes(final TestParameterBuilder<K, String, T> testParameter)
        throws Exception {

        final String topic = testParameter.getTopic();
        final QuickData<T> keyInfo = testParameter.getInfoType();

        final QuickTopicData<T, String> info = new QuickTopicData<>(
            topic,
            TopicWriteType.MUTABLE,
            keyInfo,
            TestTypeUtils.newStringData()
        );
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), schemaRegistry.getUrl());

        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());

        final TopicTypeService typeService = topicTypeService(keyInfo.getType(), QuickTopicType.STRING, null);
        final KafkaIngestService kafkaIngestService = new KafkaIngestService(typeService, kafkaConfig);

        final DataFetcher<String> mutationFetcher =
            new MutationFetcher<>(topic,
                "id",
                "name", new Lazy<>(() -> info),
                kafkaIngestService,
                objectMapper
            );

        final KeyValue<K, String> keyValue = testParameter.getKeyValue();

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", keyValue.getKey(), "name", keyValue.getValue()))
            .build();

        final String actual = mutationFetcher.get(env);

        assertThat(actual).isEqualTo(keyValue.getValue());

        final Optional<KeyValue<String, String>> consumedRecords =
            kafkaCluster.read(ReadKeyValues.from(topic, String.class)
                    .with(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyInfo.getSerde().deserializer().getClass())
                    .with(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))
                .stream().findFirst();

        assertThat(consumedRecords)
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("key", keyValue.getKey())
            .hasFieldOrPropertyWithValue("value", keyValue.getValue());

    }

    private static TopicTypeService topicTypeService(final QuickTopicType keyType,
        final QuickTopicType valueType, @Nullable final Schema valueSchema) {
        return TestTopicTypeService.builder()
            .urlSupplier(schemaRegistry::getUrl)
            .keyType(keyType)
            .valueType(valueType)
            .keySchema(null)
            .valueSchema(valueSchema)
            .build();
    }

    @Data
    @Builder
    private static class TestParameterBuilder<K, V, T> {
        private String topic;
        private KeyValue<K, V> keyValue;
        private QuickData<T> infoType;
        private Class<T> classType;
    }
}
