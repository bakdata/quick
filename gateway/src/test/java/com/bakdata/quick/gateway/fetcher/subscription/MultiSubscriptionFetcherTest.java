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


import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.TestTypeUtils;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.bakdata.quick.testutil.Chart;
import com.bakdata.quick.testutil.ClickStats;
import com.bakdata.quick.testutil.PurchaseStats;
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.subscribers.TestSubscriber;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.mguenther.kafka.junit.KeyValue;
import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

class MultiSubscriptionFetcherTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Flaky on CI")
    void shouldReturnComplexType() {
        final DataFetcherClient<String, ?> field1Client = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<String, ?> field2Client = Mockito.mock(DataFetcherClient.class);
        Mockito.doReturn("field1Key2").when(field1Client).fetchResult("key2");
        Mockito.doReturn("field2Key1").when(field2Client).fetchResult("key1");

        final SubscriptionProvider<?, ?> field1Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", "field1Key1"));
        final SubscriptionProvider<?, ?> field2Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, "key2", "field2Key2"));

        final Map<String, DataFetcherClient<String, ?>> fieldClients =
            Map.of("field1", field1Client, "field2", field2Client);
        final Map<String, SubscriptionProvider<?, ?>> fieldSubscribers =
            Map.of("field1", field1Subscriber, "field2", field2Subscriber);

        final List<String> selectedFields = List.of("field1", "field2");
        final MultiSubscriptionFetcher fetcher =
            new MultiSubscriptionFetcher(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher =
            fetcher.get(DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build());

        final TestSubscriber<Object> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();
        testSubscriber.assertValueAt(0, Map.of("field1", "field1Key1", "field2", "field2Key1"));
        testSubscriber.assertValueAt(1, Map.of("field2", "field2Key2", "field1", "field1Key2"));
    }

    @Test
    void shouldReturnSingleFieldOfComplexType() {
        final DataFetcherClient<String, ?> field1Client = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<String, ?> field2Client = Mockito.mock(DataFetcherClient.class);

        final SubscriptionProvider<?, ?> field1Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", "field1Key1"));
        final SubscriptionProvider<?, ?> field2Subscriber = env -> Flux.empty();

        final Map<String, DataFetcherClient<String, ?>> fieldClients =
            Map.of("field1", field1Client, "field2", field2Client);
        final Map<String, SubscriptionProvider<?, ?>> fieldSubscribers =
            Map.of("field1", field1Subscriber, "field2", field2Subscriber);

        final List<String> selectedFields = List.of("field1");
        final MultiSubscriptionFetcher fetcher =
            new MultiSubscriptionFetcher(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher =
            fetcher.get(DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build());

        final TestSubscriber<Object> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();
        testSubscriber.assertValueAt(0, Map.of("field1", "field1Key1"));
        Mockito.verifyNoInteractions(field1Client, field2Client);
    }

    @ParameterizedTest(name = "shouldFetchValuesForComplexType ({0})")
    @MethodSource("provideArguments")
    <K, V> void shouldFetchValuesForComplexType(final String name, final List<KeyValue<String, Pair<V, V>>> keyValues,
                             final QuickTopicData.QuickData<K> keyInfo, final QuickTopicData.QuickData<V> valueInfo,
                             final List<V> expected) throws InterruptedException {

        // Es gibt vier verschiedene Values
        // Key 1 field 1
        // Key 1 field 2
        // Key 2 field 1
        // Key 2 field 2
        // Man kännte davon ausgehen, dass F1 avro ist und F2 genauso
        final V key1field1 = keyValues.get(0).getValue().getKey();
        final V key1field2 = keyValues.get(0).getValue().getValue();
        final V key2field1 = keyValues.get(1).getValue().getKey();
        final V key2field2 = keyValues.get(1).getValue().getValue();

        final DataFetcherClient<?> field1Client = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<?> field2Client = Mockito.mock(DataFetcherClient.class);
        Mockito.doReturn(key1field2).when(field2Client).fetchResult("key1");
        Mockito.doReturn(key2field1).when(field1Client).fetchResult("key2");

        final SubscriptionProvider<?, ?> field1Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", key1field1));
        final SubscriptionProvider<?, ?> field2Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, "key2", key2field2));





    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
            Arguments.of(
                "int-avro-test",
                List.of(
                    new KeyValue<>(1, newClickStatsInputAvro(2L, 3L)),
                    new KeyValue<>(2, newClickStatsInputAvro(3L, 4L))),
                TestTypeUtils.newIntegerData(),
                TestTypeUtils.newAvroData(ChartRecord.getClassSchema()),
                List.of(
                    newAvroClickStatsOutput(2L, 3L),
                    newAvroClickStatsOutput(3L, 4L)
                )),
            Arguments.of(
                "string-proto-test",
                List.of(
                    new KeyValue<>(1, newProtoRecord(2L, 3L)),
                    new KeyValue<>(2, newProtoRecord(3L, 4L))),
                TestTypeUtils.newStringData(),
                TestTypeUtils.newProtobufData(Chart.getDescriptor()),
                List.of(
                    newProtoRecord(2L, 3L),
                    newProtoRecord(3L, 4L)
                ))
                );
    }

    private static Record newAvroClickStatsOutput(final String id, final long amount) {
        final Record record = new Record(com.bakdata.quick.avro.ClickStats.getClassSchema());
        record.put("0", id);
        record.put(1, amount);
        return record;
    }

    private static Record newAvroPurchaseStatsOutput(final String id, final long amount) {
        final Record record = new Record(com.bakdata.quick.avro.PurchaseStats.getClassSchema());
        record.put("0", id);
        record.put(10, amount);
        return record;
    }

    private static com.bakdata.quick.avro.ClickStats newClickStatsInputAvro(final String id, final long amount) {
        return com.bakdata.quick.avro.ClickStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static com.bakdata.quick.avro.PurchaseStats newPurchaseStatsInputAvro(final String id, final long amount) {
        return com.bakdata.quick.avro.PurchaseStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static Message newClickStatsRecord(final String id, final long amount) {
        return ClickStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static Message newPurchaseStatsRecord(final String id, final long amount) {
        return PurchaseStats.newBuilder().setId(id).setAmount(amount).build();
    }


}
