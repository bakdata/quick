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
 * The following example presents the correct way of providing arguments to the mutation type.
 * <pre>{@code
 * type Mutation {
 *      setProduct(id: ID, product: ProductInput): Product @topic(name: "product-topic")
 * }
 *}</pre>
 * On the other hand, the example below depicts a code example in which the rules for providing
 * arguments to the mutation type are violated (there is only a single argument whereas two are needed):
 * <pre>{@code
 * type Mutation {
 *      setClick(clickCount: Long): Long @topic(name: "click-topic")
 * }
 * }</pre>
 */
public class MutationRequiresTwoArguments implements ValidationRule {

    @Override
    public Optional<String> validate(final TopicDirectiveContext context) {
        if (context.getParentContainerName().equals(GraphQLUtils.MUTATION_TYPE)
                && context.getEnvironment().getFieldDefinition().getArguments().size() != 2) {
            return Optional.of("Mutation requires two input arguments");
        }
        return Optional.empty();
    }
}
