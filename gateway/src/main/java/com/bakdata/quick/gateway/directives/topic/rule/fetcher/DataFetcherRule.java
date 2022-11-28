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
import com.bakdata.quick.gateway.directives.topic.rule.TopicDirectiveRule;
import com.bakdata.quick.gateway.fetcher.FetcherFactory;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.FieldCoordinates;
import java.util.List;
import java.util.stream.Stream;

/**
 * Rules for setting data fetcher for a {@link com.bakdata.quick.gateway.directives.topic.TopicDirective}.
 */
public interface DataFetcherRule extends TopicDirectiveRule {

    /**
     * Returns data fetcher specifications for DeferFetcher.
     *
     * <p>
     * A {@link com.bakdata.quick.gateway.fetcher.DeferFetcher} is required in situations where the key argument is
     * passed in through a parent object. This method finds this location and adds defer fetcher so that the key
     * arguments are passed down to the annotated field.
     *
     * @see com.bakdata.quick.gateway.fetcher.DeferFetcher DeferFetcher for a in-depth explanation
     */
    default Stream<DataFetcherSpecification> extractDeferFetcher(final TopicDirectiveContext context) {
        if (context.getParentContainerName().equals(GraphQLUtils.QUERY_TYPE)) {
            return Stream.empty();
        }
        final ObjectTypeDefinition objectTypeDefinition = context.getEnvironment().getRegistry()
            .getType("Query", ObjectTypeDefinition.class)
            // this should not happen because GraphQL specs require the Query root object to be always present
            .orElseThrow(() -> new QuickDirectiveException("Query not found"));

        final Stream<String> fieldsWithParentType = objectTypeDefinition.getFieldDefinitions()
            .stream()
            .filter(field -> this.extractTypeName(field.getType()).getName().equals(context.getParentContainerName()))
            .map(FieldDefinition::getName);

        return fieldsWithParentType
            .map(field -> FieldCoordinates.coordinates(GraphQLUtils.QUERY_TYPE, field))
            .map(coords -> DataFetcherSpecification.of(coords, FetcherFactory.deferFetcher()));
    }

    /**
     * Extracts name of given type.
     */
    default TypeName extractTypeName(final Type<?> type) {
        if (type instanceof TypeName) {
            return ((TypeName) type);
        }

        if (type instanceof ListType) {
            return this.extractTypeName(((ListType) type).getType());
        }

        if (type instanceof NonNullType) {
            return this.extractTypeName(((NonNullType) type).getType());
        }

        throw new QuickDirectiveException("Found unknown type: " + type.getClass().getSimpleName());
    }

    /**
     * Extracts a list of data fetcher and their locations for this rule.
     *
     * @param context the current topic directive information
     * @return a list of data fetcher and where they should be applied
     */
    List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context);

    /**
     * Checks whether this rule can be applied for this context.
     *
     * @param context the current topic directive information
     * @return whether this rule is valid for this context
     */
    boolean isValid(final TopicDirectiveContext context);

    /**
     * Returns coordinates of field where the topic directive is applied to.
     */
    default FieldCoordinates currentCoordinates(final TopicDirectiveContext context) {
        return FieldCoordinates.coordinates(
            context.getEnvironment().getFieldsContainer(),
            context.getEnvironment().getElement()
        );
    }
}
