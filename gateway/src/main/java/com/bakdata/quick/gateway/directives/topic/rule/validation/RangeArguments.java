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
 * Validation for range queries.
 *
 * <p>
 * These rules should apply:
 * <ol>
 * <li> The Parent container should be a Query and not Mutation/Subscription
 * <li> Both rangeFrom and rangeTo fields should exist in the topic directive
 * <li> A valid keyArgument should exist in the topic directive
 * <li> The return type of the query should be list
 * </ol>
 *
 * <p>
 * <h2>Valid schema:</h2>
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
 */
public class RangeArguments implements ValidationRule {
    @Override
    public Optional<String> validate(final TopicDirectiveContext context) {
        if (hasRangeFromAndRangeTo(context)) {
            if (!context.getParentContainerName().equals(GraphQLUtils.QUERY_TYPE)) {
                return Optional.of("Range queries are only supported on Query types.");
            } else if (!context.getTopicDirective().hasKeyArgument()) {
                return Optional.of("You must define a keyArgument.");
            } else if (!context.isListType()) {
                return Optional.of("The return type of range queries should be a list.");
            }
            return ValidationUtility.makeCheckForKeyArgument(context);
        } else if (!context.getTopicDirective().hasRangeFrom() && !context.getTopicDirective().hasRangeTo()) {
            return Optional.empty();
        }
        return Optional.of("Both rangeFrom and rangeTo arguments should be set.");
    }

    private static boolean hasRangeFromAndRangeTo(final TopicDirectiveContext context) {
        return context.getTopicDirective().hasRangeFrom()
            && context.getTopicDirective().hasRangeTo();
    }
}
