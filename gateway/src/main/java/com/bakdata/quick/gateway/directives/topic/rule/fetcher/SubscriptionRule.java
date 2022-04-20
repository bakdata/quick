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

package com.bakdata.quick.gateway.directives.topic.rule.fetcher;

import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.gateway.DataFetcherSpecification;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import java.util.List;

/**
 * Rule for subscription fetcher.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Subscription {
 *     getURL(id: ID): String @topic(name: "url-topic") # <- subscription fetcher
 * }
 * }</pre>
 *
 * @see SubscriptionFetcher
 */
public class SubscriptionRule implements DataFetcherRule {
    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        final DataFetcher<?> dataFetcher = context.getFetcherFactory().subscriptionFetcher(
            context.getTopicDirective().getTopicName(),
            context.getEnvironment().getElement().getName(),
            context.getTopicDirective().getKeyArgument()
        );
        final FieldCoordinates coordinates = this.currentCoordinates(context);
        return List.of(DataFetcherSpecification.of(coordinates, dataFetcher));
    }

    @Override
    public boolean isValid(final TopicDirectiveContext context) {
        return context.getParentContainerName().equals(GraphQLUtils.SUBSCRIPTION_TYPE);
    }
}
