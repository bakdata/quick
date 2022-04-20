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

package com.bakdata.quick.gateway;

import com.bakdata.quick.gateway.directives.topic.TopicDirective;
import graphql.schema.GraphQLDirectiveContainer;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.schema.idl.SchemaPrinter;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prints GraphQL schema types.
 */
public final class TypePrinter {

    private static final SchemaPrinter schemaPrinter = new SchemaPrinter();

    private TypePrinter() {
    }

    /**
     * Prints the schema for a GraphQLType.
     *
     * <p>
     * There are two major differences compared to {@link SchemaPrinter#print(GraphQLType)}: First, this prints the
     * schema of all references types. Second, it skips fields that are annotated with {@link TopicDirective}.
     *
     * <h3>Example:</h3>
     * In the following snippet, the types purchase and price are printed, but not product since it is not part of the
     * purchase schema anymore.
     * <pre>
     * {@code
     * type Purchase  {
     *   purchaseId: ID!,
     *   priceHistory: [Price!],
     *   product: Product @topic(name: "product-topic" keyField: "productId")
     * }
     * }
     * </pre>
     * becomes
     * <pre>
     * {@code
     * type Purchase  {
     *   purchaseId: ID!,
     *   priceHistory: [Price!],
     * }
     *
     * type Price {
     *     amount: Int
     *     currency: String
     * }
     * }
     * </pre>
     *
     * @param root the GraphQLType to print
     * @return string of the type's schema
     */
    public static String printTypeSchema(final GraphQLType root) {
        final StringBuilder builder = new StringBuilder();
        final Queue<GraphQLType> workQueue = new ArrayDeque<>();
        final Set<String> printedTypes = new HashSet<>();

        workQueue.add(root);

        // 1. unwrap lists and nonnull types, e.g., [Type!] -> Type
        // 2. skip iff it is scalar or its already printed
        // 3. for object type, add types of its fields to work queue but skip those annotated with the topic directive
        // 4. print schema (for object types, again, skip field annotated with the topic directive)
        while (!workQueue.isEmpty()) {
            final GraphQLType type = workQueue.poll();
            final GraphQLUnmodifiedType unwrapped = GraphQLTypeUtil.unwrapAll(type);
            if (GraphQLTypeUtil.isScalar(unwrapped) || printedTypes.contains(unwrapped.getName())) {
                continue;
            }

            final GraphQLType printType;
            if (unwrapped instanceof GraphQLObjectType) {
                final GraphQLObjectType objectType = (GraphQLObjectType) unwrapped;
                final List<GraphQLFieldDefinition> nonAnnotatedFields = objectType.getFieldDefinitions().stream()
                    .filter(TypePrinter::isAnnotatedWithTopicDirective)
                    .collect(Collectors.toList());

                printType = objectType.transform(typeBuilder -> typeBuilder.replaceFields(nonAnnotatedFields));

                for (final GraphQLFieldDefinition field : nonAnnotatedFields) {
                    workQueue.add(field.getType());
                }
            } else {
                printType = unwrapped;
            }

            printedTypes.add(unwrapped.getName());
            builder.append(schemaPrinter.print(printType));
        }

        return builder.toString();
    }

    private static boolean isAnnotatedWithTopicDirective(final GraphQLDirectiveContainer field) {
        return field.getDirectives().stream()
            .noneMatch(directive -> directive.getName().equals(TopicDirective.DIRECTIVE_NAME));
    }
}
