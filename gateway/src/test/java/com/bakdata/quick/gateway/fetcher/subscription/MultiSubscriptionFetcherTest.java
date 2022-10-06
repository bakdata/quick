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


import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.bakdata.quick.testutil.ClickStats;
import com.bakdata.quick.testutil.PurchaseStats;
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.subscribers.TestSubscriber;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
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

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Flaky on CI")
    void shouldFetchValuesForStringKeyAndAvroValue() {

        final com.bakdata.quick.avro.ClickStats key1clickStats = newClickStatsInputAvro("key1", 1);
        final com.bakdata.quick.avro.PurchaseStats key1purchaseStats = newPurchaseStatsInputAvro("key1", 2);
        final com.bakdata.quick.avro.ClickStats key2clickStats = newClickStatsInputAvro("key2", 3);
        final com.bakdata.quick.avro.PurchaseStats key2purchaseStats = newPurchaseStatsInputAvro("key2", 4);

        final DataFetcherClient<String, com.bakdata.quick.avro.ClickStats> clickStatsClient = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<String, com.bakdata.quick.avro.PurchaseStats> purchaseStatsClient = Mockito.mock(DataFetcherClient.class);
        Mockito.doReturn(key1purchaseStats).when(purchaseStatsClient).fetchResult("key1");
        Mockito.doReturn(key2clickStats).when(clickStatsClient).fetchResult("key2");

        final SubscriptionProvider<String, com.bakdata.quick.avro.ClickStats> clickStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", key1clickStats));
        final SubscriptionProvider<String, com.bakdata.quick.avro.PurchaseStats> purchaseStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, "key2", key2purchaseStats));

        final Map<String, DataFetcherClient<String, ?>> fieldClients = Map.of(
            "field1", clickStatsClient,
            "field2", purchaseStatsClient
        );
        final Map<String, SubscriptionProvider<String, ?>> fieldSubscribers = Map.of(
            "field1", clickStatsProvider,
            "field2", purchaseStatsProvider
        );

        final List<String> selectedFields = List.of("field1", "field2");
        final MultiSubscriptionFetcher<String> fetcher =
            new MultiSubscriptionFetcher<>(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher = fetcher.get(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build()
        );

        final TestSubscriber<Object> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();

        testSubscriber.assertValueAt(0, Map.of(
            "field1", newAvroClickStatsOutput("key1", 1),
            "field2", newAvroPurchaseStatsOutput("key1", 2)
        ));
        testSubscriber.assertValueAt(0, Map.of(
            "field1", newAvroPurchaseStatsOutput("key2", 4),
            "field2", newAvroClickStatsOutput("key2", 3)
        ));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Flaky on CI")
    void shouldFetchValuesForDoubleKeyAndProtoValue() {
        final ClickStats key1clickStats = newClickStatsRecord("key1", 1);
        final PurchaseStats key1purchaseStats = newPurchaseStatsRecord("key1", 2);
        final ClickStats key2clickStats = newClickStatsRecord("key2", 3);
        final PurchaseStats key2purchaseStats = newPurchaseStatsRecord("key2", 4);

        final DataFetcherClient<Double, ClickStats> clickStatsClient = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<Double, PurchaseStats> purchaseStatsClient = Mockito.mock(DataFetcherClient.class);
        Mockito.doReturn(key1purchaseStats).when(purchaseStatsClient).fetchResult(1d);
        Mockito.doReturn(key2clickStats).when(clickStatsClient).fetchResult(2d);

        final SubscriptionProvider<Double, ClickStats> clickStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, 1d, key1clickStats));
        final SubscriptionProvider<Double, PurchaseStats> purchaseStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, 2d, key2purchaseStats));

        final Map<String, DataFetcherClient<Double, ?>> fieldClients = Map.of(
            "field1", clickStatsClient,
            "field2", purchaseStatsClient
        );
        final Map<String, SubscriptionProvider<Double, ?>> fieldSubscribers = Map.of(
            "field1", clickStatsProvider,
            "field2", purchaseStatsProvider
        );

        final List<String> selectedFields = List.of("field1", "field2");
        final MultiSubscriptionFetcher<Double> fetcher =
            new MultiSubscriptionFetcher<>(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher = fetcher.get(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build()
        );

        final TestSubscriber<Object> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();
        testSubscriber.assertValueAt(0, Map.of(
            "field1", newProtoClickStatsOutput("key1", 1),
            "field2", newProtoPurchaseStatsOutput("key1", 2)));
        testSubscriber.assertValueAt(1, Map.of(
            "field2", newProtoPurchaseStatsOutput("key2", 4),
            "field1", newProtoClickStatsOutput("key2", 3)));
    }

    private static GenericRecord newAvroClickStatsOutput(final String id, final long amount) {
        return newClickStatsInputAvro(id, amount);
    }

    private static GenericRecord newAvroPurchaseStatsOutput(final String id, final long amount) {
        return newPurchaseStatsInputAvro(id, amount);
    }

    private static com.bakdata.quick.avro.ClickStats newClickStatsInputAvro(final String id, final long amount) {
        return com.bakdata.quick.avro.ClickStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static com.bakdata.quick.avro.PurchaseStats newPurchaseStatsInputAvro(final String id, final long amount) {
        return com.bakdata.quick.avro.PurchaseStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static ClickStats newClickStatsRecord(final String id, final long amount) {
        return ClickStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static PurchaseStats newPurchaseStatsRecord(final String id, final long amount) {
        return PurchaseStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static Message newProtoClickStatsOutput(final String id, final long amount) {
        return ClickStats.newBuilder().setId(id).setAmount(amount).build();
    }

    private static Message newProtoPurchaseStatsOutput(final String id, final long amount) {
        return PurchaseStats.newBuilder().setId(id).setAmount(amount).build();
    }




}
