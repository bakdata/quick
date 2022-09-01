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
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLTypeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rule for list argument fetcher.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Query {
 *  findPurchases(purchaseId: [ID]): [Purchase] @topic(name: "purchase-topic", keyArgument: "purchaseId")
 *  # <- list argument fetcher
 * }
 *
 * type Purchase  {
 *  purchaseId: ID!,
 *  productId: ID!,
 * }
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.fetcher.ListArgumentFetcher
 */
public class ListArgumentFetcherRule implements DataFetcherRule {
    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        final List<DataFetcherSpecification> specifications = new ArrayList<>();

        Objects.requireNonNull(context.getTopicDirective().getKeyArgument());
        final DataFetcher<?> dataFetcher = context.getFetcherFactory().listArgumentFetcher(
            context.getTopicDirective().getTopicName(),
            context.getTopicDirective().getKeyArgument(),
            context.isNullable(),
            context.isHasNullableElements()
        );
        final FieldCoordinates coordinates = this.currentCoordinates(context);
        specifications.add(DataFetcherSpecification.of(coordinates, dataFetcher));

        return specifications;
    }

    @Override
    public boolean isValid(final TopicDirectiveContext context) {
        return context.getTopicDirective().getKeyArgument() != null
            && context.isListType()
            && !context.getParentContainerName().equals(GraphQLUtils.SUBSCRIPTION_TYPE)
            && GraphQLTypeUtil.isList(context.getEnvironment().getElement().getType());
    }
}
