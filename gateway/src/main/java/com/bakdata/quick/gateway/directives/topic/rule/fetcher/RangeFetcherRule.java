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
import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphqlElementParentTree;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rule for range query fetcher.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Query {
 *     userRequests(
 *         userId: Int
 *         timestampFrom: Int
 *         timestampTo: Int
 *     ): [UserRequests] @topic(name: "user-request-range",
 *                              keyArgument: "userId",
 *                              rangeFrom: "timestampFrom",
 *                              rangeTo: "timestampTo")
 * }
 *
 * type UserRequests {
 *     userId: Int
 *     serviceId: Int
 *     timestamp: Int
 *     requests: Int
 *     success: Int
 * }
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.fetcher.RangeQueryFetcher
 */
public class RangeFetcherRule implements DataFetcherRule {
    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        Objects.requireNonNull(context.getTopicDirective().getKeyArgument());
        Objects.requireNonNull(context.getTopicDirective().getRangeFrom());
        Objects.requireNonNull(context.getTopicDirective().getRangeTo());

        TypeName typeName = extractKeyArgumentType(context, context.getTopicDirective().getKeyArgument());

        final DataFetcher<?> dataFetcher = context.getFetcherFactory().rangeFetcher(
            context.getTopicDirective().getTopicName(),
            context.getTopicDirective().getKeyArgument(),
            context.getTopicDirective().getRangeFrom(),
            context.getTopicDirective().getRangeTo(),
            context.isNullable(),
            typeName
        );
        final FieldCoordinates coordinates = this.currentCoordinates(context);
        return List.of(DataFetcherSpecification.of(coordinates, dataFetcher));
    }

    @Override
    public boolean isValid(final TopicDirectiveContext context) {
        return context.getTopicDirective().hasKeyArgument()
            && context.getTopicDirective().hasRangeFrom()
            && context.getTopicDirective().hasRangeTo()
            && !context.getParentContainerName().equals(GraphQLUtils.SUBSCRIPTION_TYPE)
            && context.getParentContainerName().equals(GraphQLUtils.QUERY_TYPE)
            && GraphQLTypeUtil.isList(context.getEnvironment().getElement().getType());
    }

    private static TypeName extractKeyArgumentType(final TopicDirectiveContext context, final String keyArgument) {
        final Optional<GraphqlElementParentTree> parentInfo = context.getEnvironment()
            .getElementParentTree()
            .getParentInfo();

        if (parentInfo.isEmpty()) {
            throw new QuickDirectiveException("Could not find the parent object type.");
        }

        final GraphQLSchemaElement element = parentInfo.get().getElement();
        final ObjectTypeDefinition definition = ((GraphQLObjectType) element).getDefinition();

        final List<InputValueDefinition> inputValueDefinitions =
            definition.getFieldDefinitions().get(0).getInputValueDefinitions();

        final Optional<InputValueDefinition> field = inputValueDefinitions.stream()
            .filter(inputValueDefinition -> inputValueDefinition.getName().equals(keyArgument))
            .findFirst();

        if (field.isEmpty()) {
            final String errorMessage =
                String.format("Could not find the keyArgument %s in the parent type definition. Please check your schema.",
                    keyArgument);
            throw new QuickDirectiveException(errorMessage);
        }
        return DataFetcherRule.extractName(field.get().getType());
    }
}
