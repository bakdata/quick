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

package com.bakdata.quick.gateway.directives.topic;

import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;

import com.bakdata.quick.gateway.directives.QuickDirective;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.language.DirectiveDefinition;
import graphql.language.DirectiveLocation;
import graphql.language.InputValueDefinition;
import graphql.schema.GraphQLArgument;
import java.util.Collection;
import java.util.Objects;
import lombok.Getter;

/**
 * Custom GraphQL directive enabling integration of GraphQL and Kafka.
 *
 * <p>
 * Corresponds to the following GraphQL definition:
 * <pre>{@code
 * directive @topic(
 *     name: String!,
 *     keyArgument: String,
 *     keyField: String,
 *     rangeFrom: String,
 *     rangeTo: String
 * ) on FIELD_DEFINITION
 * }</pre>
 */
@Getter
public final class TopicDirective implements QuickDirective {
    public static final DirectiveDefinition DEFINITION;
    public static final String DIRECTIVE_NAME = "topic";

    private static final String TOPIC_NAME_ARG_NAME = "name";
    private static final String KEY_ARGUMENT_ARG_NAME = "keyArgument";
    private static final String KEY_FIELD_ARG_NAME = "keyField";
    private static final String RANGE_FROM_ARG_NAME = "rangeFrom";
    private static final String RANGE_TO_ARG_NAME = "rangeTo";

    static {
        DEFINITION = DirectiveDefinition.newDirectiveDefinition()
            .name(DIRECTIVE_NAME)
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(TOPIC_NAME_ARG_NAME)
                    .type(NON_NULL_STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(KEY_ARGUMENT_ARG_NAME)
                    .type(STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(KEY_FIELD_ARG_NAME)
                    .type(STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(RANGE_FROM_ARG_NAME)
                    .type(STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(RANGE_TO_ARG_NAME)
                    .type(STRING)
                    .build())
            .directiveLocation(
                DirectiveLocation.newDirectiveLocation()
                    .name(FIELD_DEFINITION.name())
                    .build())
            .build();
    }

    private final String topicName;
    @Nullable
    private final String keyArgument;
    @Nullable
    private final String keyField;
    @Nullable
    private final String rangeFrom;
    @Nullable
    private final String rangeTo;

    private TopicDirective(final String topicName, @Nullable final String keyArgument,
        @Nullable final String keyField, @Nullable final String rangeFrom, @Nullable final String rangeTo) {
        Objects.requireNonNull(topicName);
        this.topicName = topicName;
        this.keyArgument = keyArgument;
        this.keyField = keyField;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
    }

    /**
     * Creates a topic directive from schema.
     */
    public static TopicDirective fromArguments(final Collection<? extends GraphQLArgument> arguments) {
        final String topicName = Objects.requireNonNull(QuickDirective.extractArgument(arguments, TOPIC_NAME_ARG_NAME),
            "Topic name in topic directive must be set");
        final String keyArgument = QuickDirective.extractArgument(arguments, KEY_ARGUMENT_ARG_NAME);
        final String keyField = QuickDirective.extractArgument(arguments, KEY_FIELD_ARG_NAME);
        final String rangeFrom = QuickDirective.extractArgument(arguments, RANGE_FROM_ARG_NAME);
        final String rangeTo = QuickDirective.extractArgument(arguments, RANGE_TO_ARG_NAME);
        return new TopicDirective(topicName, keyArgument, keyField, rangeFrom, rangeTo);
    }

    public boolean hasKeyArgument() {
        return this.keyArgument != null;
    }

    public boolean hasKeyField() {
        return this.keyField != null;
    }
    public boolean hasRangeFrom() {
        return this.rangeFrom != null;
    }
    public boolean hasRangeTo() {
        return this.rangeTo != null;
    }
}
