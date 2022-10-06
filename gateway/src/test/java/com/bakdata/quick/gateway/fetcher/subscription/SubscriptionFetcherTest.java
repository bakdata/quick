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

package com.bakdata.quick.gateway.fetcher.subscription;

import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bakdata.quick.common.TestTypeUtils;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.subscription.KafkaSubscriptionProvider.OffsetStrategy;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.subscribers.TestSubscriber;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.SendKeyValuesTransactional;
import net.mguenther.kafka.junit.TopicConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

class SubscriptionFetcherTest {
    private static final EmbeddedKafkaCluster kafkaCluster = provisionWith(defaultClusterConfig());

    @BeforeAll
    static void setup() {
        kafkaCluster.start();
    }

    @AfterAll
    static void afterAll() {
        kafkaCluster.stop();
    }

    @ParameterizedTest(name = "shouldFetchValues ({0})")
    @MethodSource("provideValueArguments")
    <V> void shouldFetchValues(final String topic, final List<KeyValue<String, V>> keyValues,
        final QuickData<V> valueInfo, final List<V> expected)
        throws InterruptedException {
        final QuickTopicData<String, V> info = new QuickTopicData<>(
            topic,
            TopicWriteType.IMMUTABLE,
            TestTypeUtils.newStringData(),
            valueInfo
        );
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), "http://no");

        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());

        final SendKeyValuesTransactional<String, V> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, keyValues)
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueInfo.getSerde().serializer().getClass())
            .build();

        kafkaCluster.send(sendRequest);

        final SubscriptionFetcher<String, V>
            subscriptionFetcher = new SubscriptionFetcher<>(kafkaConfig, new Lazy<>(() -> info), "test-query",
            OffsetStrategy.EARLIEST, null);
        final Publisher<V> publisher =
            subscriptionFetcher.get(DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build());

        final TestSubscriber<V> testSubscriber = TestSubscriber.create();
        // ensures that multiple subscriber work
        final TestSubscriber<V> test2Subscriber = TestSubscriber.create();
        publisher.subscribe(testSubscriber);
        publisher.subscribe(test2Subscriber);

        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                assertThat(testSubscriber.values()).containsExactlyElementsOf(expected);
                assertThat(test2Subscriber.values()).containsExactlyElementsOf(expected);
            });
        deleteTopic(topic);
    }

    @ParameterizedTest(name = "shouldFetchValuesForGivenKey ({0})")
    @MethodSource("provideValueArgumentsForKey")
    <K, V> void shouldFetchValuesForGivenKey(final String topic, final List<KeyValue<K, V>> keyValues,
        final QuickData<K> keyInfo, final QuickData<V> valueInfo, final K key, final List<V> expected)
        throws InterruptedException {
        final QuickTopicData<K, V> info = new QuickTopicData<>(
            topic,
            TopicWriteType.IMMUTABLE,
            keyInfo,
            valueInfo
        );
        final KafkaConfig kafkaConfig = new KafkaConfig(kafkaCluster.getBrokerList(), "http://no");

        kafkaCluster.createTopic(TopicConfig.withName(topic).useDefaults());

        final SendKeyValuesTransactional<K, V> sendRequest = SendKeyValuesTransactional
            .inTransaction(topic, keyValues)
            .with(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keyInfo.getSerde().serializer().getClass())
            .with(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueInfo.getSerde().serializer().getClass())
            .build();

        kafkaCluster.send(sendRequest);

        final String argumentName = "id";
        final SubscriptionFetcher<K, V> subscriptionFetcher =
            new SubscriptionFetcher<>(kafkaConfig, new Lazy<>(() -> info), "key-query", OffsetStrategy.EARLIEST,
                argumentName);
        final DataFetchingEnvironment fetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of(argumentName, key))
            .build();
        final Publisher<V> publisher = subscriptionFetcher.get(fetchingEnvironment);
        final TestSubscriber<V> testSubscriber = TestSubscriber.create();
        publisher.subscribe(testSubscriber);

        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(testSubscriber.values()).containsExactlyElementsOf(expected));
        deleteTopic(topic);
    }

    private static Stream<Arguments> provideValueArguments() {
        return Stream.of(
            // integer
            Arguments.of(
                "integer-test-values-test",
                keyValuePairsWithRandomKey(List.of(2, 5, 81243)),
                TestTypeUtils.newIntegerData(),
                List.of(2, 5, 81243)
            ),
            // long
            Arguments.of(
                "long-test-values-test",
                keyValuePairsWithRandomKey(List.of(2L, 5L, 81243L)),
                TestTypeUtils.newLongData(),
                List.of(2L, 5L, 81243L)
            ),
            // double
            Arguments.of(
                "double-test-values-test",
                keyValuePairsWithRandomKey(List.of(2.0, 5.0, 81243.0)),
                TestTypeUtils.newDoubleData(),
                List.of(2.0, 5.0, 81243.0)
            ),
            // string
            Arguments.of(
                "string-test-values-test",
                keyValuePairsWithRandomKey(List.of("2", "5", "81243")),
                TestTypeUtils.newStringData(),
                List.of("2", "5", "81243")
            )
        );
    }

    private static Stream<Arguments> provideValueArgumentsForKey() {
        return Stream.of(
            // integer
            Arguments.of(
                "integer-test-key-test",
                keyValuePairFromValue(List.of(2, 5, 81243)),
                TestTypeUtils.newIntegerData(),
                TestTypeUtils.newIntegerData(),
                5,
                List.of(5)
            ),
            // long
            Arguments.of(
                "long-test-key-test",
                keyValuePairFromValue(List.of(2L, 5L, 81243L)),
                TestTypeUtils.newLongData(),
                TestTypeUtils.newLongData(),
                5L,
                List.of(5L)
            ),
            // double
            Arguments.of(
                "double-test-key-test",
                keyValuePairFromValue(List.of(2.0, 5.0, 81243.0)),
                TestTypeUtils.newDoubleData(),
                TestTypeUtils.newDoubleData(),
                5.0,
                List.of(5.0)
            ),
            // string
            Arguments.of(
                "string-test-key-test",
                keyValuePairFromValue(List.of("2", "5", "81243")),
                TestTypeUtils.newStringData(),
                TestTypeUtils.newStringData(),
                "5",
                List.of("5")
            )
        );
    }

    private static <V> List<KeyValue<String, V>> keyValuePairsWithRandomKey(final List<V> vs) {
        return vs.stream()
            .map(e -> new KeyValue<>(RandomStringUtils.random(5), e))
            .collect(Collectors.toList());
    }

    private static <V> List<KeyValue<V, V>> keyValuePairFromValue(final List<V> vs) {
        return vs.stream()
            .map(e -> new KeyValue<>(e, e))
            .collect(Collectors.toList());
    }

    private static void deleteTopic(final String topic) {
        if (kafkaCluster.exists(topic)) {
            kafkaCluster.deleteTopic(topic);
        }
    }
}
