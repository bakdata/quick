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

package com.bakdata.quick.common.type.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.TestTopicRegistryClient;
import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.mirror.TopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.schema.SchemaFetcher;
import com.bakdata.quick.common.schema.SchemaFormat;
import com.bakdata.quick.common.schema.SchemaRegistryFetcher;
import com.bakdata.quick.common.type.DefaultConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.testutil.ChartRecord;
import com.bakdata.quick.testutil.ProtoTestRecord;
import com.bakdata.schemaregistrymock.SchemaRegistryMock;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.reactivex.Single;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QuickTopicTypeServiceTest {
    private final SchemaRegistryMock schemaRegistryMock =
        new SchemaRegistryMock(List.of(new AvroSchemaProvider(), new ProtobufSchemaProvider()));
    private final TopicRegistryClient topicRegistryClient = new TestTopicRegistryClient();

    @BeforeEach
    void setUp() {
        this.schemaRegistryMock.start();
    }

    @AfterEach
    void tearDown() {
        this.schemaRegistryMock.stop();
    }

    @ParameterizedTest
    @MethodSource("topicDataValueArguments")
    <K, V> void shouldReturnValueTopicData(final TopicData topicData, @Nullable final ParsedSchema schema,
                                           final SchemaProvider schemaProvider) {
        final TopicTypeService typeService = this.createTopicTypeService(schemaProvider);

        if (schema != null) {
            this.schemaRegistryMock.registerValueSchema(topicData.getName(), schema);
        }
        this.topicRegistryClient.register(topicData.getName(), topicData).blockingAwait();

        final Single<QuickTopicData<K, V>> quickData = typeService.getTopicData(topicData.getName());
        final QuickTopicData.QuickData<V> valueData = quickData.blockingGet().getValueData();
        assertThat(valueData.getType()).isEqualTo(topicData.getValueType());
        assertThat(valueData.getResolver()).isNotNull();
        assertThat(valueData.getSerde()).isNotNull();
    }


    @ParameterizedTest
    @MethodSource("topicDataKeyArguments")
    <K, V> void shouldReturnKeyTopicData(final TopicData topicData, @Nullable final ParsedSchema schema,
                                         final SchemaProvider schemaProvider) {
        final TopicTypeService typeService = this.createTopicTypeService(schemaProvider);

        if (schema != null) {
            this.schemaRegistryMock.registerKeySchema(topicData.getName(), schema);
        }
        this.topicRegistryClient.register(topicData.getName(), topicData).blockingAwait();

        final Single<QuickTopicData<K, V>> quickData = typeService.getTopicData(topicData.getName());
        final QuickTopicData.QuickData<K> keyData = quickData.blockingGet().getKeyData();
        assertThat(keyData.getType()).isEqualTo(topicData.getKeyType());
        assertThat(keyData.getResolver()).isNotNull();
        assertThat(keyData.getSerde()).isNotNull();
    }

    private static Stream<Arguments> topicDataValueArguments() {
        return topicDataArguments(
            type -> new TopicData("test", TopicWriteType.MUTABLE, QuickTopicType.STRING, type, null)
        );
    }

    private static Stream<Arguments> topicDataKeyArguments() {
        return topicDataArguments(
            type -> new TopicData("test", TopicWriteType.MUTABLE, type, QuickTopicType.STRING, null)
        );
    }

    private static Stream<Arguments> topicDataArguments(final Function<QuickTopicType, TopicData> creator) {
        return Stream.of(
            Arguments.of(creator.apply(QuickTopicType.DOUBLE), null, null),
            Arguments.of(creator.apply(QuickTopicType.INTEGER), null, null),
            Arguments.of(creator.apply(QuickTopicType.STRING), null, null),
            Arguments.of(creator.apply(QuickTopicType.LONG), null, null),
            Arguments.of(creator.apply(QuickTopicType.AVRO), new AvroSchema(ChartRecord.getClassSchema()),
                new AvroSchemaProvider()),
            Arguments.of(creator.apply(QuickTopicType.PROTOBUF),
                new ProtobufSchema(ProtoTestRecord.getDescriptor()), new ProtobufSchemaProvider())
        );
    }

    private TopicTypeService createTopicTypeService(final SchemaProvider schemaProvider) {
        final KafkaConfig kafkaConfig = new KafkaConfig("dummy:123", this.schemaRegistryMock.getUrl());
        final SchemaFetcher schemaFetcher = new SchemaRegistryFetcher(new HttpClient(), kafkaConfig, schemaProvider);
        final SchemaConfig schemaConfig = new SchemaConfig(Optional.of(SchemaFormat.AVRO), Optional.empty());
        final DefaultConversionProvider conversionProvider = new DefaultConversionProvider(schemaConfig, kafkaConfig);
        return new QuickTopicTypeService(schemaFetcher, this.topicRegistryClient, conversionProvider);
    }
}
