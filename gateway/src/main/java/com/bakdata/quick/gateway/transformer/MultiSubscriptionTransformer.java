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

package com.bakdata.quick.gateway.transformer;

import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.gateway.DataFetcherSpecification;
import com.bakdata.quick.gateway.directives.topic.TopicDirective;
import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.bakdata.quick.gateway.fetcher.FetcherFactory;
import com.bakdata.quick.gateway.fetcher.subscription.MultiSubscriptionFetcher;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionProvider;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGeneratorPostProcessing;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Post-processing for creating subscriptions of multiple topics.
 *
 * <p>
 * This is implemented as {@link SchemaGeneratorPostProcessing} because the fields itself doesn't have a directive
 * attached to it.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Subscription {
 *     getStatistics: Statistics # <- Multi-Subscription Fetcher
 * }
 *
 * type Statistics {
 *     clickStatistics: ClickStatistics @topic(name: "user-statistics")
 *     purchaseStatistics: PurchaseStatistics @topic(name: "purchase-statistics")
 * }
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.directives.topic.rule.fetcher.SubscriptionRule
 */
@Slf4j
@Singleton
public class MultiSubscriptionTransformer implements SchemaGeneratorPostProcessing {
    private final FetcherFactory fetcherFactory;

    public MultiSubscriptionTransformer(final FetcherFactory fetcherFactory) {
        this.fetcherFactory = fetcherFactory;
    }

    @Override
    public GraphQLSchema process(final GraphQLSchema schema) {
        if (schema.getSubscriptionType() == null) {
            return schema;
        }

        final List<DataFetcherSpecification> fetchers = schema.getSubscriptionType().getFieldDefinitions().stream()
            // Skip fields that are handled by the SubscriptionRule
            .filter(field -> !field.getDirectivesByName().containsKey("topic")
                && field.getType() instanceof GraphQLObjectType)
            .map(this::buildDataFetcher)
            .collect(Collectors.toList());

        final GraphQLCodeRegistry codeRegistry = schema.getCodeRegistry().transform(builder ->
            fetchers.forEach(spec -> builder.dataFetcher(spec.getCoordinates(), spec.getDataFetcher()))
        );

        return schema.transform(builder -> builder.codeRegistry(codeRegistry));
    }

    /**
     * Build a {@link MultiSubscriptionFetcher} for each field of the complex type that a subscription field returns.
     *
     * @param fieldDefinition field of the subscription type
     * @return a data fetcher with its field coordinates
     */
    private <K> DataFetcherSpecification buildDataFetcher(final GraphQLFieldDefinition fieldDefinition) {
        final GraphQLObjectType objectType = (GraphQLObjectType) fieldDefinition.getType();

        final Map<String, DataFetcherClient<K, ?>> dataFetchers = new HashMap<>();
        final Map<String, SubscriptionProvider<K, ?>> subscriptionProviders = new HashMap<>();

        for (final GraphQLFieldDefinition field : objectType.getFieldDefinitions()) {
            if (!field.getDirectivesByName().containsKey(TopicDirective.DIRECTIVE_NAME)) {
                log.warn("Skip field {} of {} in subscription: No topic directive found", field.getName(),
                    objectType.getName());
                continue;
            }

            final List<GraphQLArgument> arguments = field.getDirective(TopicDirective.DIRECTIVE_NAME).getArguments();
            final TopicDirective topicDirective = TopicDirective.fromArguments(arguments);

            final DataFetcherClient<K, ?> dataFetcherClient =
                this.fetcherFactory.dataFetcherClient(topicDirective.getTopicName());
            final SubscriptionProvider<K, ?> subscriptionProvider =
                this.fetcherFactory.subscriptionProvider(topicDirective.getTopicName(), field.getName(),
                    topicDirective.getKeyArgument());

            dataFetchers.put(field.getName(), dataFetcherClient);
            subscriptionProviders.put(field.getName(), subscriptionProvider);
        }

        final DataFetcher<?> multiSubscriptionFetcher =
            new MultiSubscriptionFetcher<>(dataFetchers, subscriptionProviders);
        final FieldCoordinates coordinates =
            FieldCoordinates.coordinates(GraphQLUtils.SUBSCRIPTION_TYPE, fieldDefinition.getName());

        return DataFetcherSpecification.of(coordinates, multiSubscriptionFetcher);
    }

}
