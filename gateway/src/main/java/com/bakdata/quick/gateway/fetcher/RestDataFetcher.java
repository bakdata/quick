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

package com.bakdata.quick.gateway.fetcher;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.directives.rest.RestParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * {@link DataFetcher} for arbitrary rest services.
 *
 * <p>
 * This data fetcher extracts the field by requesting the data from a rest service with a given base url. For each
 * query, it constructs the url from the arguments given in that query. For that reason, it is initialized with the
 * names of arguments for that used for the path and the query respectively.
 *
 * <p>
 * For now, it supports only rest services that return valid JSON objects (not lists!).
 *
 * @param <T> return type
 */
@Slf4j
public class RestDataFetcher<T> implements DataFetcher<T> {
    private static final JavaType OBJECT_TYPE =
        TypeFactory.defaultInstance().constructType(new TypeReference<Map<String, Object>>() {
        });
    private final HttpClient client;
    private final HttpUrl url;
    private final List<RestParameter> pathArguments;
    private final List<RestParameter> queryArguments;
    @Nullable
    private final RestParameter bodyParameter;
    private final boolean isNullable;
    private final RestDirectiveMethod methodType;

    /**
     * Default constructor.
     *
     * @param client         quick http client
     * @param url            base url of the underlying rest service
     * @param pathParameter  names of the GraphQL arguments that are used in the http path
     * @param queryParameter names of the GraphQL arguments that are used in the http query
     * @param bodyParameter  name of the GraphQL argument that is used as the HTTP body
     * @param isNullable     true if this data fetcher's return type is nullable
     * @param methodType     HTTP method to use
     */
    public RestDataFetcher(final HttpClient client, final String url, final List<RestParameter> pathParameter,
                           final List<RestParameter> queryParameter, @Nullable final RestParameter bodyParameter,
                           final boolean isNullable, final RestDirectiveMethod methodType) {
        this.client = client;
        this.url = Objects.requireNonNull(HttpUrl.parse(url), "No REST-Url given");
        this.pathArguments = pathParameter;
        this.queryArguments = queryParameter;
        this.bodyParameter = bodyParameter;
        this.isNullable = isNullable;
        this.methodType = methodType;
    }

    @Override
    @Nullable
    public T get(final DataFetchingEnvironment environment) throws IOException {
        final HttpUrl.Builder urlBuilder = this.url.newBuilder();

        // extract all path arguments and add them to it
        // order matters!
        for (final RestParameter pathArgument : this.pathArguments) {
            final String pathValue = this.extractRestValue(environment, pathArgument)
                .orElseThrow(() -> makeMissingPathParamError(environment, pathArgument));
            urlBuilder.addPathSegment(pathValue);
        }

        // same as for the path
        for (final RestParameter queryArgument : this.queryArguments) {
            final Optional<String> queryValue = this.extractRestValue(environment, queryArgument);
            queryValue.ifPresent(value -> urlBuilder.addQueryParameter(queryArgument.getName(), value));
        }

        final Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
        final Request request = this.setRequestMethod(environment, requestBuilder).build();

        log.debug("Request send: {}", request);
        try (final Response response = this.client.newCall(request).execute()) {
            return this.handleResponse(environment, response);
        }
    }

    @Nullable
    private T handleResponse(final DataFetchingEnvironment environment, final Response response) throws IOException {
        log.debug("Response received: {}", response);
        if (response.code() == HttpStatus.NOT_FOUND.getCode() && this.isNullable) {
            return null;
        }

        final ResponseBody body = response.body();
        if (response.code() != HttpStatus.OK.getCode()) {
            final GraphqlErrorException.Builder builder = GraphqlErrorException.newErrorException()
                .message("REST service responded with code " + response.code())
                .sourceLocation(environment.getFieldDefinition().getDefinition().getSourceLocation());

            if (body != null) {
                builder.extensions(Map.of("REST error message", body.string()));
            }

            throw builder.build();
        }

        if (body == null || body.contentLength() == 0) {
            throw GraphqlErrorException.newErrorException()
                .message("The REST service didn't respond with a body")
                .sourceLocation(environment.getFieldDefinition().getDefinition().getSourceLocation())
                .build();
        }

        return this.client.objectMapper().readValue(body.bytes(), OBJECT_TYPE);
    }

    private Request.Builder setRequestMethod(final DataFetchingEnvironment environment,
                                             final Request.Builder requestBuilder)
        throws JsonProcessingException {
        if (this.methodType == RestDirectiveMethod.GET) {
            requestBuilder.get();
        } else if (this.methodType == RestDirectiveMethod.POST) {
            if (this.bodyParameter != null) {
                this.extractRestValue(environment, this.bodyParameter)
                    .map(json -> RequestBody.create(json, HttpClient.JSON))
                    .ifPresent(requestBuilder::post);
            } else {
                requestBuilder.post(RequestBody.create(new byte[] {}));
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + this.methodType);
        }

        return requestBuilder;
    }

    public List<RestParameter> getPathArguments() {
        return this.pathArguments;
    }

    public List<RestParameter> getQueryArguments() {
        return this.queryArguments;
    }

    public RestDirectiveMethod getMethodType() {
        return this.methodType;
    }

    @Nullable
    public RestParameter getBodyParameter() {
        return this.bodyParameter;
    }

    private static GraphqlErrorException makeMissingPathParamError(final DataFetchingEnvironment environment,
                                                                   final RestParameter restParameter) {
        final String message = String.format(
            "%s is used as a path parameter and is thus required. Either specify it in your query or remove it as a "
                + "path parameter.",
            restParameter.getName());
        return GraphqlErrorException.newErrorException()
            .message(message)
            .sourceLocation(environment.getFieldDefinition().getDefinition().getSourceLocation())
            .build();
    }

    /**
     * Utility for extracting the argument from the GraphQL query and converting it to a string.
     */
    private Optional<String> extractRestValue(final DataFetchingEnvironment environment,
                                              final RestParameter queryArgument)
        throws JsonProcessingException {
        final T argumentValue = environment.getArgument(queryArgument.getName());
        if (argumentValue == null) {
            return Optional.empty();
        }

        final String queryValue;
        if (queryArgument.isPrimitive()) {
            queryValue = argumentValue.toString();
        } else {
            queryValue = this.client.objectMapper().writeValueAsString(argumentValue);
        }
        return Optional.of(queryValue);
    }
}
