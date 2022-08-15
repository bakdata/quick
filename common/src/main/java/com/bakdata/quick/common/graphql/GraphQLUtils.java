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

package com.bakdata.quick.common.graphql;

import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.type.QuickTopicType;
import graphql.Scalars;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Utility class for common GraphQL operations.
 */
public final class GraphQLUtils {
    // name of the query, the subscription, and the mutation type for the new GraphQL schema
    // value can not be changed as our GraphQL expects these names
    public static final String QUERY_TYPE = "Query";
    public static final String SUBSCRIPTION_TYPE = "Subscription";
    public static final String MUTATION_TYPE = "Mutation";
    // maps primitives topic types (all except avro) to their respective scalar in GraphQL
    public static final Map<QuickTopicType, String> TYPE_TO_GQL_SCALAR_NAME_MAP = typeToScalarNameMap();

    /**
     * Not instantiatable because its a utility class.
     */
    private GraphQLUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Checks whether two TypeDefinitions are the same.
     *
     * <p>
     * This is necessary as we consider equality when the fields are equals. {@link TypeDefinition#equals(Object)} also
     * considers source location. However, this is obviously not always required in our case.
     *
     * @param leftType  left type definition
     * @param rightType right type definition
     * @return true if all fields/values are equals, or they are exactly the same type
     */
    public static boolean areSameTypes(final TypeDefinition<?> leftType, final TypeDefinition<?> rightType) {
        if (leftType instanceof ObjectTypeDefinition && rightType instanceof ObjectTypeDefinition) {
            final List<FieldDefinition> leftDefinitions = ((ObjectTypeDefinition) leftType).getFieldDefinitions();
            final List<FieldDefinition> rightDefinitions = ((ObjectTypeDefinition) rightType).getFieldDefinitions();
            return leftDefinitions.containsAll(rightDefinitions) && rightDefinitions.containsAll(leftDefinitions);
        }
        if (leftType instanceof EnumTypeDefinition && rightType instanceof EnumTypeDefinition) {
            final List<EnumValueDefinition> leftDefinitions = ((EnumTypeDefinition) leftType).getEnumValueDefinitions();
            final List<EnumValueDefinition> rightDefinitions =
                ((EnumTypeDefinition) rightType).getEnumValueDefinitions();
            return leftDefinitions.containsAll(rightDefinitions) && rightDefinitions.containsAll(leftDefinitions);
        }
        return leftType.equals(rightType);
    }

    /**
     * Returns the name of the type in a topic backed by this registry.
     *
     * @param registry registry backing a topic
     * @return name of the type
     */
    public static String getRootType(final QuickTopicType type, final TypeDefinitionRegistry registry) {
        if (type != QuickTopicType.SCHEMA) {
            return Objects.requireNonNull(TYPE_TO_GQL_SCALAR_NAME_MAP.get(type));
        }

        final Map<String, Integer> objectFiledCount = new HashMap<>();
        final List<String> skippableTypes = new ArrayList<>();

        // graphql-java returns a raw type of TypeDefintion
        @SuppressWarnings("unchecked") final Map<String, TypeDefinition<?>> types =
            (Map<String, TypeDefinition<?>>) (Map<String, ?>) registry.types();
        for (final Entry<String, ? extends TypeDefinition<?>> next : types.entrySet()) {
            final String typeName = next.getKey();
            final TypeDefinition<?> typeDefinition = next.getValue();
            if (typeName.equals(QUERY_TYPE) || skippableTypes.contains(typeName)) {
                // we can skip the query type and type we've already seen
                continue;
            }
            if (typeDefinition instanceof ObjectTypeDefinition) {
                final ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) typeDefinition;
                final List<FieldDefinition> fieldDefinitions = objectTypeDefinition.getFieldDefinitions();
                final int objectTypesAsFieldsCount = (int) fieldDefinitions.stream()
                    .map(FieldDefinition::getType)
                    .map(GraphQLUtils::getNameOfType)
                    .filter(types::containsKey)
                    .map(skippableTypes::add) // seen as field => can not be root object
                    .count();
                objectFiledCount.put(typeName, objectTypesAsFieldsCount);
            } else {
                objectFiledCount.put(typeName, 0);
            }
        }
        return Collections.max(objectFiledCount.entrySet(), Entry.comparingByValue()).getKey();
    }

    /**
     * Extracts the name of the given type.
     *
     * <p>
     * Required as we need to work around container types like list type and non-null type.
     *
     * @param type type to extract the name from
     * @return name of the type
     */
    public static String getNameOfType(final Type<?> type) {
        if (type instanceof TypeName) {
            return ((TypeName) type).getName();
        } else if (type instanceof ListType) {
            return getNameOfType(((ListType) type).getType());
        } else if (type instanceof NonNullType) {
            return getNameOfType(((NonNullType) type).getType());
        } else {
            throw new BadArgumentException("Can not get name of type implementation " + type.getClass());
        }
    }

    // helper for creating TypeName easier
    public static TypeName namedType(final String name) {
        return TypeName.newTypeName(name).build();
    }

    /**
     * Returns mapping from primitive topic types to the names of the corresponding scalars.
     */
    private static Map<QuickTopicType, String> typeToScalarNameMap() {
        return Map.of(
            QuickTopicType.STRING, Scalars.GraphQLString.getName(),
            QuickTopicType.LONG, ExtendedScalars.GraphQLLong.getName(),
            QuickTopicType.INTEGER, Scalars.GraphQLInt.getName(),
            QuickTopicType.DOUBLE, Scalars.GraphQLFloat.getName()
        );
    }
}
