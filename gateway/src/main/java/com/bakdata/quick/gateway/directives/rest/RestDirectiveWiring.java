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

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.directives.QuickDirectiveWiring;
import com.bakdata.quick.gateway.fetcher.RestDataFetcher;
import graphql.language.DirectiveDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;

/**
 * A directive wiring for {@link RestDirective}.
 *
 * <p>
 * The annotated field's data fetcher gets updated to be a {@link RestDataFetcher}.
 * This class main task is to extract the path and query parameter for the Http call. This is based on the directive
 * itself. The user can set names that they want to use as query, path and body parameters. If this is not set, we use
 * the following convention: Required arguments in order of their appearance form the path of the url. Non-required
 * arguments are used as query parameter. Here the order doesn't matter.
 */
@Singleton
public class RestDirectiveWiring implements QuickDirectiveWiring {
    private final HttpClient httpClient;

    public RestDirectiveWiring(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public GraphQLFieldDefinition onField(final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        final RestDirective restDirective = RestDirective.fromArguments(environment.getDirective().getArguments());

        // first, check if rest directive can work
        validateDirective(environment.getFieldDefinition(), restDirective);
        // then update the field's data fetcher to a rest data fetcher
        environment.getCodeRegistry().dataFetcher(
            fieldCoordinates(environment),
            this.createRestDataFetcher(environment, restDirective)
        );

        return environment.getElement();
    }

    @Override
    public DirectiveDefinition getDefinition() {
        return RestDirective.DEFINITION;
    }

    @Override
    public String getName() {
        return RestDirective.DIRECTIVE_NAME;
    }

    /**
     * Validates if the rest directive placement is valid.
     */
    private static void validateDirective(final GraphQLFieldDefinition fieldDefinition,
                                          final RestDirective restDirective) {
        // we don't support Rest Apis that return lists, it must be wrapped in an object
        if (GraphQLTypeUtil.isList(GraphQLTypeUtil.unwrapNonNull(fieldDefinition.getType()))) {
            throw new QuickDirectiveException("The return type of a field with a rest directive must not be a list");
        }

        // we expect that the Rest Apis DON'T return scalars but valid JSON
        if (GraphQLTypeUtil.isScalar(GraphQLTypeUtil.unwrapAll(fieldDefinition.getType()))) {
            throw new QuickDirectiveException("The return type of a field with a rest directive must not be a scalar");
        }

        // make sure the parameters use distinct arguments
        // e.g., there is an argument "id" and the user has defined it as path AND query argument
        final List<String> duplicates = getDuplicatedParameters(restDirective);
        if (!duplicates.isEmpty()) {
            final String errorMessage = String.format("The argument(s) %s can't be used in multiple parameters",
                String.join(",", duplicates));
            throw new QuickDirectiveException(errorMessage);
        }

        final String bodyParameter = restDirective.getBodyParameter();
        if (restDirective.getRestDirectiveMethod() == RestDirectiveMethod.GET
            && bodyParameter != null) {
            throw new QuickDirectiveException("A body parameter is only supported for POST requests");
        }

        if (bodyParameter != null) {
            final GraphQLArgument graphQLArgument = fieldDefinition.getArguments().stream()
                .filter(field -> field.getName().equals(bodyParameter))
                .findFirst()
                .orElseThrow(() -> new QuickDirectiveException(
                    String.format("Field for body parameter %s not found", bodyParameter)));

            if (GraphQLTypeUtil.isScalar(GraphQLTypeUtil.unwrapAll(graphQLArgument.getType()))) {
                throw new QuickDirectiveException("The type of the body parameter must not be a scalar");
            }
        }
    }

    /**
     * Helper utility for getting the field coordinates of the current field.
     */
    private static FieldCoordinates fieldCoordinates(
        final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        return FieldCoordinates.coordinates(
            environment.getFieldsContainer(),
            environment.getElement()
        );
    }

    /**
     * Helper utility for creating a {@link RestParameter} from a {@link GraphQLArgument}.
     */
    private static RestParameter getRestArgument(final GraphQLArgument arg) {
        final GraphQLUnmodifiedType unwrappedType = GraphQLTypeUtil.unwrapAll(arg.getType());
        return new RestParameter(arg.getName(), GraphQLTypeUtil.isScalar(unwrappedType)
            || GraphQLTypeUtil.isEnum(unwrappedType));
    }

    /**
     * Returns a list of names that appear in multiple user defined parameters.
     */
    private static List<String> getDuplicatedParameters(final RestDirective restDirective) {
        final List<String> duplicateParameters = new ArrayList<>();
        final Set<String> uniqueParameters = new HashSet<>();
        final Stream<String> parameters = Stream.of(
            restDirective.getPathParameters().stream(),
            restDirective.getQueryParameters().stream(),
            Stream.ofNullable(restDirective.getBodyParameter())
        ).flatMap(Function.identity());

        parameters.forEach(parameter -> {
            if (!uniqueParameters.add(parameter)) {
                duplicateParameters.add(parameter);
            }
        });
        return duplicateParameters;
    }

    /**
     * Returns all Rest parameter for a given parameter type.
     *
     * <p>
     * By default, we extract the parameter by convention:
     * <ol>
     *  <li> NonNull arguments of the field are path parameter
     *  <li> Nullable arguments of the field are query parameter
     * </ol>
     *
     * <p>
     * The user can override this behavior by setting the {@link RestDirective}'s arguments {@code pathParameters}
     * {@code queryParameters}, and {@code bodyParameter} respectively. In these cases we use the arguments specified
     * there as the parameter. If a field is manually requested, it is omitted from all other parameter types.
     *
     * @param argumentMap    the arguments of the field, accessible by their name
     * @param fieldArguments list of field arguments. Despite having the arguments in the map already, we need them as a
     *                       list since the order matters for path parameters.
     * @param userParameters the parameters specified by the user in the directive
     * @param parameterType  type of the parameters to get
     * @return list of arguments forwarded to the rest service for the given parameter type
     */
    private static List<RestParameter> getRestParameters(final Map<String, GraphQLArgument> argumentMap,
                                                         final List<GraphQLArgument> fieldArguments,
                                                         final Map<ParameterType, List<String>> userParameters,
                                                         final ParameterType parameterType) {

        final List<String> fieldNames = Objects.requireNonNull(userParameters.get(parameterType));
        if (fieldNames.isEmpty()) {
            return fieldArguments.stream()
                .filter(argument -> parameterType.isOfParameterType(argument, userParameters))
                .map(RestDirectiveWiring::getRestArgument)
                .collect(Collectors.toList());
        } else {
            return fieldNames.stream()
                .map(argName -> Objects.requireNonNull(argumentMap.get(argName),
                    String.format("Argument %s specified as parameter but not found", argName)))
                .map(RestDirectiveWiring::getRestArgument)
                .collect(Collectors.toList());
        }
    }

    /**
     * Returns a {@link RestDataFetcher} based on directive defined in the schema and the field's arguments.
     *
     * @param environment   the field's environment
     * @param restDirective the parsed directive as defined in the schema
     * @return a rest data fetcher
     */
    private DataFetcher<?> createRestDataFetcher(
        final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment, final RestDirective restDirective) {

        final GraphQLFieldDefinition fieldDefinition = environment.getFieldDefinition();
        // create argument map for more efficient access
        final List<GraphQLArgument> arguments = fieldDefinition.getArguments();
        final Map<String, GraphQLArgument> argumentMap = arguments.stream().collect(
            Collectors.toMap(GraphQLArgument::getName, Function.identity())
        );

        final Map<ParameterType, List<String>> userArguments = Map.of(
            ParameterType.PATH, restDirective.getPathParameters(),
            ParameterType.QUERY, restDirective.getQueryParameters(),
            ParameterType.BODY, Stream.ofNullable(restDirective.getBodyParameter()).collect(Collectors.toList())
        );

        final List<RestParameter> pathParams =
            getRestParameters(argumentMap, arguments, userArguments, ParameterType.PATH);
        final List<RestParameter> queryParams =
            getRestParameters(argumentMap, arguments, userArguments, ParameterType.QUERY);

        final RestParameter bodyParameter = Optional.ofNullable(restDirective.getBodyParameter())
            .map(argumentMap::get)
            .map(RestDirectiveWiring::getRestArgument)
            .orElse(null);

        // do we allow returning null?
        final boolean nullable = GraphQLTypeUtil.isNullable(fieldDefinition.getType());

        return new RestDataFetcher<>(this.httpClient, restDirective.getUrl(), pathParams, queryParams, bodyParameter,
            nullable, restDirective.getRestDirectiveMethod());
    }


    /**
     * Type for differentiating between path and query parameter in a Http call.
     */
    @SuppressWarnings("ImmutableEnumChecker")
    enum ParameterType {
        // By default, non-null arguments are used in the path
        PATH(argument -> GraphQLTypeUtil.isNonNull(argument.getType())),
        // By default, nullable arguments are query parameters
        QUERY(argument -> !GraphQLTypeUtil.isNonNull(argument.getType())),
        // By default, no arguments is used as a body parameter
        BODY(argument -> false);

        private final Predicate<GraphQLArgument> argumentPredicate;

        ParameterType(final Predicate<GraphQLArgument> argumentPredicate) {
            this.argumentPredicate = argumentPredicate;
        }

        /**
         * Returns true if the given argument is of this type.
         */
        public boolean isOfParameterType(final GraphQLArgument argument,
                                         final Map<ParameterType, List<String>> userArguments) {
            // Check if the argument is manually requested for another type. This has precedence.
            final boolean isUsedElsewhere = userArguments.entrySet().stream()
                .filter(entry -> entry.getKey() != this)
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(userParameter -> userParameter.equals(argument.getName()));

            if (isUsedElsewhere) {
                return false;
            } else {
                return this.argumentPredicate.test(argument);
            }
        }

    }

}
