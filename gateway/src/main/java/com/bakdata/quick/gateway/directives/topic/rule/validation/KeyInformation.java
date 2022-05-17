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
import java.util.Optional;

/**
 * Validation for {@link com.bakdata.quick.gateway.directives.topic.TopicDirective}
 *
 * <p>
 * When a user declares a non-mutation or a non-subscription type with a return type that is not a list,
 * they have to provide key information - either keyField or keyArgument.
 * For example:
 * type Product {
 *     id: ID!
 *     name: String!
 * }
 * type Query {
 *     getProduct(id: ID): Product @topic(name: "product-topic", keyArgument: "id")
 * }
 */
public class KeyInformation implements ValidationRule {

    @Override
    public Optional<String> validate(final TopicDirectiveContext context) {
        if (!context.getParentContainerName().equals(GraphQLUtils.MUTATION_TYPE)
                && !context.getParentContainerName().equals(GraphQLUtils.SUBSCRIPTION_TYPE)
                && !context.isListType()
                && !context.getTopicDirective().hasKeyArgument()
                && !context.getTopicDirective().hasKeyField()) {
            return Optional.of("When the return type is not a list for a non-mutation and non-subscription type,"
                    + " key information (keyArgument or keyField) is needed");
        }
        return Optional.empty();
    }
}
