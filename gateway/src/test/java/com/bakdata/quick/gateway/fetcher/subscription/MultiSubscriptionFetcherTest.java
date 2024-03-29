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


import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.bakdata.quick.avro.ClickStatsAvro;
import com.bakdata.quick.avro.PurchaseStatsAvro;
import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.bakdata.quick.testutil.ClickStatsProto;
import com.bakdata.quick.testutil.PurchaseStatsProto;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.subscribers.TestSubscriber;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
        final DataFetcherClient<String, ?> field1Client = mock(DataFetcherClient.class);
        final DataFetcherClient<String, ?> field2Client = mock(DataFetcherClient.class);
        doReturn("field1Key2").when(field1Client).fetchResult("key2");
        doReturn("field2Key1").when(field2Client).fetchResult("key1");

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
        final DataFetcherClient<String, ?> field1Client = mock(DataFetcherClient.class);
        final DataFetcherClient<String, ?> field2Client = mock(DataFetcherClient.class);

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
        final ClickStatsAvro key1clickStats = newClickStatsAvro("key1", 1);
        final PurchaseStatsAvro key1purchaseStats = newPurchaseStatsAvro("key1", 2);
        final ClickStatsAvro key2clickStats = newClickStatsAvro("key2", 3);
        final PurchaseStatsAvro key2purchaseStats = newPurchaseStatsAvro("key2", 4);

        final DataFetcherClient<String, ClickStatsAvro> clickStatsClient = mock(DataFetcherClient.class);
        final DataFetcherClient<String, PurchaseStatsAvro> purchaseStatsClient = mock(DataFetcherClient.class);
        doReturn(key1purchaseStats).when(purchaseStatsClient).fetchResult("key1");
        doReturn(key2clickStats).when(clickStatsClient).fetchResult("key2");

        final SubscriptionProvider<String, ClickStatsAvro> clickStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", key1clickStats));
        final SubscriptionProvider<String, PurchaseStatsAvro> purchaseStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, "key2", key2purchaseStats));

        final Map<String, DataFetcherClient<String, ?>> fieldClients = Map.of(
            "clicks", clickStatsClient,
            "purchases", purchaseStatsClient
        );
        final Map<String, SubscriptionProvider<String, ?>> fieldSubscribers = Map.of(
            "clicks", clickStatsProvider,
            "purchases", purchaseStatsProvider
        );

        final List<String> selectedFields = List.of("clicks", "purchases");
        final MultiSubscriptionFetcher<String> fetcher =
            new MultiSubscriptionFetcher<>(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher = fetcher.get(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build()
        );

        final TestSubscriber<Map<String, ?>> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();

        final Map<String, Object> expected = new HashMap<>();
        expected.put("clicks", newClickStatsAvro("key1", 1));
        expected.put("purchases", newPurchaseStatsAvro("key1", 2));
        testSubscriber.assertValueAt(0, expected);

        final Map<String, Object> expected2 = new HashMap<>();
        expected2.put("clicks", newClickStatsAvro("key2", 3));
        expected2.put("purchases", newPurchaseStatsAvro("key2", 4));
        testSubscriber.assertValueAt(1, expected2);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Flaky on CI")
    void shouldFetchValuesForDoubleKeyAndProtoValue() {
        final ClickStatsProto key1clickStats = newClickStatsProto("key1", 1);
        final PurchaseStatsProto key1purchaseStats = newPurchaseStatsProto("key1", 2);
        final ClickStatsProto key2clickStats = newClickStatsProto("key2", 3);
        final PurchaseStatsProto key2purchaseStats = newPurchaseStatsProto("key2", 4);

        final DataFetcherClient<Integer, ClickStatsProto> clickStatsClient = mock(DataFetcherClient.class);
        final DataFetcherClient<Integer, PurchaseStatsProto> purchaseStatsClient = mock(DataFetcherClient.class);
        doReturn(key1purchaseStats).when(purchaseStatsClient).fetchResult(1);
        doReturn(key2clickStats).when(clickStatsClient).fetchResult(2);

        final SubscriptionProvider<Integer, ClickStatsProto> clickStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, 1, key1clickStats));
        final SubscriptionProvider<Integer, PurchaseStatsProto> purchaseStatsProvider =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, 2, key2purchaseStats));

        final Map<String, DataFetcherClient<Integer, ?>> fieldClients = Map.of(
            "clicks", clickStatsClient,
            "purchases", purchaseStatsClient
        );
        final Map<String, SubscriptionProvider<Integer, ?>> fieldSubscribers = Map.of(
            "clicks", clickStatsProvider,
            "purchases", purchaseStatsProvider
        );

        final List<String> selectedFields = List.of("clicks", "purchases");
        final MultiSubscriptionFetcher<Integer> fetcher =
            new MultiSubscriptionFetcher<>(fieldClients, fieldSubscribers, env -> selectedFields);

        final Publisher<Map<String, Object>> mapPublisher = fetcher.get(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build()
        );

        final TestSubscriber<Object> testSubscriber = TestSubscriber.create();
        mapPublisher.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
        testSubscriber.assertComplete();
        testSubscriber.assertValueAt(0, Map.of(
            "clicks", newClickStatsProto("key1", 1),
            "purchases", newPurchaseStatsProto("key1", 2)));
        testSubscriber.assertValueAt(1, Map.of(
            "purchases", newPurchaseStatsProto("key2", 4),
            "clicks", newClickStatsProto("key2", 3)));
    }

    private static ClickStatsAvro newClickStatsAvro(final String id, final long amount) {
        return ClickStatsAvro.newBuilder().setId(id).setAmount(amount).build();
    }

    private static PurchaseStatsAvro newPurchaseStatsAvro(final String id, final long amount) {
        return PurchaseStatsAvro.newBuilder().setId(id).setAmount(amount).build();
    }

    private static ClickStatsProto newClickStatsProto(final String id, final long amount) {
        return ClickStatsProto.newBuilder().setId(id).setAmount(amount).build();
    }

    private static PurchaseStatsProto newPurchaseStatsProto(final String id, final long amount) {
        return PurchaseStatsProto.newBuilder().setId(id).setAmount(amount).build();
    }
}
