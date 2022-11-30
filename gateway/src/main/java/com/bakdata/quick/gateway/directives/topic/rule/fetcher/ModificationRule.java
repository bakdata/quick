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
import graphql.language.ListType;
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

/**
 * Rule for a modification.
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
public class ModificationRule implements DataFetcherRule {

    /**
     * Extracts a list of data fetcher for this rule. The type of the keyField needs to be checked and extracted for the
     * DataFetcher (i.e., the {@link com.bakdata.quick.gateway.fetcher.KeyFieldFetcher}). The fetcher needs to
     * distinguish if the keyField is a type of integer or long. When receiving the JSON response from the mirror it is
     * not possible determine from the JSON if the keyField is a long or integer. Therefore, the type is extracted
     * before and the fetcher uses the {@link TypeName} to cast the JSON value into Java integer or long.
     *
     * @param context the current topic directive information
     */
    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        final String keyField = context.getTopicDirective().getKeyField();
        Objects.requireNonNull(keyField);

        final TypeName type = this.extractKeyFieldType(context, keyField);

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

    /**
     * Extracts the {@link TypeName} of a given {@link Type}. This class does not use the default implementation of the
     * interface because the MutationRule should not parse ListType. The Modification rule returns a single object,
     * therefore the keyField should be a scalar and not a list.
     *
     * <p>
     * Please refer to {@link ModificationListRule} for list types.
     */
    @Override
    public TypeName extractTypeName(final Type<?> type) {
        if (type instanceof ListType) {
            throw new QuickDirectiveException(
                "The topic directive is set on a field with a non list type. The keyField type should not be a list. "
                    + "Please consider using scalar a type.");
        }

        if (type instanceof TypeName) {
            return (TypeName) type;
        }

        if (type instanceof NonNullType) {
            return this.extractTypeName(((NonNullType) type).getType());
        }
        final String errorMessage =
            String.format("Found unsupported type %s for keyField. Only scalars are supported.",
                type.getClass().getSimpleName());
        throw new QuickDirectiveException(errorMessage);
    }

    private TypeName extractKeyFieldType(final TopicDirectiveContext context, final String keyField) {
        final Optional<GraphqlElementParentTree> parentInfo = context.getEnvironment()
            .getElementParentTree()
            .getParentInfo();

        if (parentInfo.isEmpty()) {
            throw new QuickDirectiveException("Could not find the parent object type.");
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
            throw new QuickDirectiveException(errorMessage);
        }
        return this.extractTypeName(field.get().getType());
    }
}
