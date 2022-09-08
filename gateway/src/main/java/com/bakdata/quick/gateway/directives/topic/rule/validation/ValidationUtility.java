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
import java.util.Objects;
import java.util.Optional;

/**
 * Utilities to validate the topic directive
 */
public final class ValidationUtility {

    private ValidationUtility() {
    }

    /**
     * Checks if the keyArgument of a topic directive is valid or not
     *
     * @param context Topic directive context
     * @return Optional empty if the keyArgument is valid
     */
    public static Optional<String> makeCheckForKeyArgument(final TopicDirectiveContext context) {
        if (context.getTopicDirective().getKeyArgument() != null) {
            final boolean inputNameAndKeyArgsMatch;
            if (context.getParentContainerName().equals(GraphQLUtils.QUERY_TYPE)) {
                inputNameAndKeyArgsMatch = checkIfInputNameAndKeyArgMatchInQueryType(context);
            } else {
                inputNameAndKeyArgsMatch = checkIfInputNameAndKeyArgMatchInNonQueryType(context);
            }
            if (!inputNameAndKeyArgsMatch) {
                return Optional.of("Key argument has to be identical to the input name.");
            }
        }
        return Optional.empty();
    }

    private static boolean checkIfInputNameAndKeyArgMatchInQueryType(final TopicDirectiveContext context) {
        final String keyArg = context.getTopicDirective().getKeyArgument();
        return context.getEnvironment().getElement().getDefinition().getInputValueDefinitions()
            .stream()
            .map(InputValueDefinition::getName)
            .anyMatch(name -> Objects.equals(name, keyArg));
    }

    private static boolean checkIfInputNameAndKeyArgMatchInNonQueryType(final TopicDirectiveContext context) {
        final Optional<?> queryTypeDef = context.getEnvironment().getRegistry().getType("Query");
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
