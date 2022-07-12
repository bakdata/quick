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

package com.bakdata.quick.gateway.directives.rest;

import static graphql.introspection.Introspection.DirectiveLocation.FIELD_DEFINITION;

import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethodType;
import com.bakdata.quick.gateway.directives.QuickDirective;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.language.DirectiveDefinition;
import graphql.language.DirectiveLocation;
import graphql.language.EnumValue;
import graphql.language.InputValueDefinition;
import graphql.language.ListType;
import graphql.language.TypeName;
import graphql.schema.GraphQLArgument;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * Custom GraphQL directive for allowing integration of arbitrary REST services.
 *
 * <p>
 * Corresponds to the following GraphQL definition:
 * <pre>{@code
 * directive @rest(
 *    url: String!
 *    pathParameter: [String!]
 *    queryParameter: [String!]
 *    bodyParameter: String
 *    httpMethod: RestDirectiveMethod = GET
 * ) on FIELD_DEFINITION
 * }</pre>
 */
@Getter
public final class RestDirective implements QuickDirective {
    public static final DirectiveDefinition DEFINITION;

    public static final String DIRECTIVE_NAME = "rest";

    public static final String URL_ARG_NAME = "url";
    public static final String QUERY_PARAMETER_ARG_NAME = "queryParameter";
    public static final String PATH_PARAMETER_ARG_NAME = "pathParameter";
    public static final String BODY_PARAMETER_ARG_NAME = "bodyParameter";
    public static final String HTTP_METHOD_PARAMETER_ARG_NAME = "httpMethod";

    private static final ListType LIST_STRING = new ListType(NON_NULL_STRING);

    static {
        DEFINITION = DirectiveDefinition.newDirectiveDefinition()
            .name(DIRECTIVE_NAME)
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(URL_ARG_NAME)
                    .type(NON_NULL_STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(PATH_PARAMETER_ARG_NAME)
                    .type(LIST_STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(QUERY_PARAMETER_ARG_NAME)
                    .type(LIST_STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(BODY_PARAMETER_ARG_NAME)
                    .type(STRING)
                    .build())
            .inputValueDefinition(
                InputValueDefinition.newInputValueDefinition()
                    .name(HTTP_METHOD_PARAMETER_ARG_NAME)
                    .type(new TypeName(RestDirectiveMethodType.TYPE_NAME))
                    .defaultValue(EnumValue.newEnumValue(RestDirectiveMethod.GET.name()).build())
                    .build())
            .directiveLocation(
                DirectiveLocation.newDirectiveLocation()
                    .name(FIELD_DEFINITION.name())
                    .build())
            .build();
    }

    private final String url;
    private final List<String> pathParameters;
    private final List<String> queryParameters;
    @Nullable
    private final String bodyParameter;
    private final RestDirectiveMethod restDirectiveMethod;

    private RestDirective(final String url, @Nullable final List<String> pathParameters,
                          @Nullable final List<String> queryParameters, @Nullable final String bodyParameter,
                          final RestDirectiveMethod restDirectiveMethod) {
        this.url = url;
        this.pathParameters = Objects.requireNonNullElse(pathParameters, Collections.emptyList());
        this.queryParameters = Objects.requireNonNullElse(queryParameters, Collections.emptyList());
        this.bodyParameter = bodyParameter;
        this.restDirectiveMethod = restDirectiveMethod;
    }

    /**
     * Creates a new RestDirective based on the arguments given in the schema.
     */
    public static RestDirective fromArguments(final Collection<? extends GraphQLArgument> arguments) {
        final String url = Objects.requireNonNull(QuickDirective.extractArgument(arguments, URL_ARG_NAME),
            "Url in rest directive must be set");
        final List<String> pathParameter = QuickDirective.extractArgument(arguments, PATH_PARAMETER_ARG_NAME);
        final List<String> queryParameter = QuickDirective.extractArgument(arguments, QUERY_PARAMETER_ARG_NAME);
        final String bodyParameter = QuickDirective.extractArgument(arguments, BODY_PARAMETER_ARG_NAME);
        final RestDirectiveMethod method = RestDirectiveMethod.valueOf(Objects.requireNonNullElse(
            QuickDirective.extractArgument(arguments, HTTP_METHOD_PARAMETER_ARG_NAME),
            RestDirectiveMethod.GET.name() // this should be the default as specified in the definition
        ));
        return new RestDirective(url, pathParameter, queryParameter, bodyParameter, method);
    }
}
