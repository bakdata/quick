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

import com.bakdata.quick.gateway.DataFetcherSpecification;
import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import graphql.language.FieldDefinition;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphqlElementParentTree;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Rule for a keyField.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Query {
 *  findPurchases: [Purchase] @topic(name: "purchase-topic")
 * }
 *
 * type Purchase  {
 *  purchaseId: ID!,
 *  productId: ID!,
 *  products: Product @topic(name: "product-topic", keyField: "productId") # <- modification
 * }
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.fetcher.KeyFieldFetcher
 */
@Slf4j
public class ModificationRule implements DataFetcherRule {

    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        final String keyField = context.getTopicDirective().getKeyField();
        Objects.requireNonNull(keyField);

        final TypeName type = extractKeyFieldType(context, keyField);
        final DataFetcher<?> dataFetcher = context.getFetcherFactory().keyFieldFetcher(
            context.getTopicDirective().getTopicName(),
            keyField,
            type
        );
        final FieldCoordinates coordinates = this.currentCoordinates(context);
        return List.of(DataFetcherSpecification.of(coordinates, dataFetcher));
    }

    @Override
    public boolean isValid(final TopicDirectiveContext context) {
        return context.getTopicDirective().hasKeyField() && !context.isListType();
    }

    private static TypeName extractKeyFieldType(final TopicDirectiveContext context, final String keyField) {
        final Optional<GraphqlElementParentTree> parentInfo = context.getEnvironment()
            .getElementParentTree()
            .getParentInfo();

        if (parentInfo.isEmpty()) {
            final String errorMessage = "Could not find the parent object type.";
            log.error(errorMessage);
            throw new QuickDirectiveException(errorMessage);
        }

        final GraphQLSchemaElement element = parentInfo.get().getElement();
        final ObjectTypeDefinition definition = ((GraphQLObjectType) element).getDefinition();

        final Optional<FieldDefinition> field = definition.getFieldDefinitions().stream()
            .filter(fieldDefinition -> fieldDefinition.getName().equals(keyField))
            .findFirst();

        if (field.isEmpty()) {
            final String errorMessage =
                String.format("Could not find the keyField %s in the parent type definition. Please check your schema.",
                    keyField);
            log.error(errorMessage);
            throw new QuickDirectiveException(
                errorMessage);
        }
        return extractType(field.get().getType());
    }

    private static TypeName extractType(final Type<?> type) {
        if (type instanceof TypeName) {
            return (TypeName) type;
        }

        if (type instanceof NonNullType) {
            return extractType(((NonNullType) type).getType());
        }

        final String errorMessage = "Found unknown type: " + type.getClass().getSimpleName();
        log.error(errorMessage);
        throw new QuickDirectiveException(errorMessage);
    }
}
