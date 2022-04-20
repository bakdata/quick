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
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.subscribers.TestSubscriber;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

@Slf4j
class MultiSubscriptionFetcherTest {

    @Test
    void shouldReturnComplexType() {
        final DataFetcherClient<?> field1Client = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<?> field2Client = Mockito.mock(DataFetcherClient.class);
        Mockito.doReturn("field1Key2").when(field1Client).fetchResult("key2");
        Mockito.doReturn("field2Key1").when(field2Client).fetchResult("key1");

        final SubscriptionProvider<?, ?> field1Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", "field1Key1"));
        final SubscriptionProvider<?, ?> field2Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic2", 0, 0, "key2", "field2Key2"));

        final Map<String, DataFetcherClient<?>> fieldClients = Map.of("field1", field1Client, "field2", field2Client);
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
        final DataFetcherClient<?> field1Client = Mockito.mock(DataFetcherClient.class);
        final DataFetcherClient<?> field2Client = Mockito.mock(DataFetcherClient.class);

        final SubscriptionProvider<?, ?> field1Subscriber =
            env -> Flux.just(new ConsumerRecord<>("topic1", 0, 0, "key1", "field1Key1"));
        final SubscriptionProvider<?, ?> field2Subscriber = env -> Flux.empty();

        final Map<String, DataFetcherClient<?>> fieldClients = Map.of("field1", field1Client, "field2", field2Client);
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
}
