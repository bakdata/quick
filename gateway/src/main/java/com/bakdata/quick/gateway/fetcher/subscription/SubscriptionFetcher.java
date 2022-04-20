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

import static com.bakdata.quick.gateway.fetcher.subscription.KafkaSubscriptionProvider.OffsetStrategy;

import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;

/**
 * A SubscriptionFetcher fetches data directly from Kafka.
 *
 * <p>
 * The fetcher creates a {@link Publisher}, which can be subscribed to. In the current architecture, {@link
 * io.micronaut.configuration.graphql.ws.GraphQLWsSender} takes the created publisher and emits the element into a
 * websocket. The websocket represents is created by the GraphQL Subscription system.
 *
 * @see io.micronaut.configuration.graphql.ws.GraphQLWsSender
 */
public class SubscriptionFetcher<K, V> implements DataFetcher<Publisher<V>> {
    final SubscriptionProvider<K, V> subscriptionProvider;

    /**
     * Creates a new SubscriptionFetcher.
     *
     * @param kafkaConfig settings concerning bootstrap server and schema registry url
     * @param info        topic information
     * @param queryName   name of the query the subscription is for
     * @param autoOffset  offset strategy
     * @param key         key to filter on - can be null.
     */
    public SubscriptionFetcher(final KafkaConfig kafkaConfig, final Lazy<QuickTopicData<K, V>> info,
        final String queryName, final OffsetStrategy autoOffset, @Nullable final String key) {
        this.subscriptionProvider = new KafkaSubscriptionProvider<>(kafkaConfig, info, queryName, autoOffset, key);
    }

    public SubscriptionFetcher(final KafkaConfig kafkaConfig, final Lazy<QuickTopicData<K, V>> info,
        final String queryName, @Nullable final String key) {
        this.subscriptionProvider = new KafkaSubscriptionProvider<>(kafkaConfig, info, queryName, key);
    }

    @Override
    public Publisher<V> get(final DataFetchingEnvironment environment) {
        return this.subscriptionProvider.getElementStream(environment).map(ConsumerRecord::value);
    }
}
