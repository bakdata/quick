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

package com.bakdata.quick.gateway.directives.topic.rule.validation;

import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import java.util.Optional;
import java.util.Objects;

/**
 * Validation for {@link com.bakdata.quick.gateway.directives.topic.TopicDirective}
 *
 * <p>
 * When a user declares a non-mutation or a non-subscription type with a return type that is not a list,
 * they have to provide key information - either keyField or keyArgument.
 * The following example presents the correct way of providing key information (key argument is present
 * when the return type is not a list, and it is the same as the input name (id):
 * <pre>{@code
 * type Product {
 *     id: ID!
 *     name: String!
 * }
 * type Query {
 *     getProduct(id: ID): Product @topic(name: "product-topic", keyArgument: "id")
 * }
}</pre>
 * On the other hand, the example below depicts a code example in which the rules for providing
 * key information are violated (the directive in the product field does not have the key argument at all,
 * and the one in the url field has an incorrect value of the key argument - it should be productId):
 * <pre>{@code
 * type Query {
 *     getProduct(productId: ID): ProductInfo
 * }
 * type ProductInfo {
 *      product: Product @topic(name: "product-topic")
 *      url: String @topic(name: "url-topic", keyArgument: "id")
 * }
 * }</pre>
 */
public class KeyInformation implements ValidationRule {

    @Override
    public Optional<String> validate(final TopicDirectiveContext context) {
        if (this.checkIfBasicContextPropertiesAreInvalid(context)) {
            return Optional.of("When the return type is not a list for a non-mutation and non-subscription type,"
                    + " key information (keyArgument or keyField) is needed.");
        }
        // additional check for key arguments
        final Optional<String> additionalKeyArgCheckResult = makeCheckForKeyArgument(context);
        if (additionalKeyArgCheckResult.isPresent()) {
            return additionalKeyArgCheckResult;
        }
        // TODO: additional check for key field
        return Optional.empty();
    }

    private Optional<String> makeCheckForKeyArgument(final TopicDirectiveContext context) {
        if (context.getTopicDirective().getKeyArgument() != null) {
            final boolean inputNameAndKeyArgsMatch;
            if (context.getParentContainerName().equals(GraphQLUtils.QUERY_TYPE)) {
                inputNameAndKeyArgsMatch = this.checkIfInputNameAndKeyArgMatchInQueryType(context);
            } else {
                inputNameAndKeyArgsMatch = this.checkIfInputNameAndKeyArgMatchInNonQueryType(context);
            }
            if (!inputNameAndKeyArgsMatch) {
                return Optional.of("Key argument has to be identical to the input name.");
            }
        }
        return Optional.empty();
    }

    private boolean checkIfBasicContextPropertiesAreInvalid(final TopicDirectiveContext context) {
        return !context.getParentContainerName().equals(GraphQLUtils.MUTATION_TYPE)
                && !context.getParentContainerName().equals(GraphQLUtils.SUBSCRIPTION_TYPE)
                && !context.isListType()
                && !context.getTopicDirective().hasKeyArgument()
                && !context.getTopicDirective().hasKeyField();
    }

    private boolean checkIfInputNameAndKeyArgMatchInQueryType(final TopicDirectiveContext context) {
        final String keyArg = context.getTopicDirective().getKeyArgument();
        return context.getEnvironment().getElement().getDefinition().getInputValueDefinitions()
                .stream()
                .map(InputValueDefinition::getName)
                .anyMatch(name -> Objects.equals(name, keyArg));

    }

    private boolean checkIfInputNameAndKeyArgMatchInNonQueryType(final TopicDirectiveContext context) {
        final Optional<TypeDefinition> queryTypeDef = context.getEnvironment().getRegistry().getType("Query");
        if (queryTypeDef.isEmpty()) {
            throw new IllegalStateException("Something went wrong - The query type is mandatory.");
        } else {
            final String keyArg = context.getTopicDirective().getKeyArgument();
            // We can cast here because we know that Query must be of the type ObjectTypeDefinition
            return ((ObjectTypeDefinition) queryTypeDef.get()).getFieldDefinitions()
                    .stream()
                    .flatMap(fieldDef -> fieldDef.getInputValueDefinitions().stream())
                    .map(InputValueDefinition::getName)
                    .anyMatch(name -> Objects.equals(name, keyArg));

        }
    }
}
