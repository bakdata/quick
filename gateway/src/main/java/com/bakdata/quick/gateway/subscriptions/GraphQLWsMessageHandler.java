/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bakdata.quick.gateway.subscriptions;

import static com.bakdata.quick.gateway.subscriptions.GraphQLWsController.HTTP_REQUEST_KEY;
import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_CONNECTION_ACK;
import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_CONNECTION_KEEP_ALIVE;
import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_ERROR;

import graphql.ExecutionResult;
import io.micronaut.configuration.graphql.GraphQLExecutionResultHandler;
import io.micronaut.configuration.graphql.GraphQLInvocation;
import io.micronaut.configuration.graphql.GraphQLInvocationData;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.ws.GraphQLWsConfiguration;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * Handles the messages send over the websocket.
 *
 * Changes to original code: Support for filters
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
@Slf4j
public class GraphQLWsMessageHandler {

    private final GraphQLWsConfiguration graphQLWsConfiguration;
    private final GraphQLWsState state;
    private final GraphQLInvocation graphQLInvocation;
    private final GraphQLExecutionResultHandler graphQLExecutionResultHandler;
    private final GraphQLWsSender responseSender;
    private final List<GraphQLWsFilter> filters;

    /**
     * Default constructor.
     *
     * @param graphQLWsConfiguration        the {@link GraphQLWsConfiguration} instance
     * @param state                         the {@link GraphQLWsState} instance
     * @param invocation                    the {@link GraphQLInvocation} instance
     * @param graphQLExecutionResultHandler the {@link GraphQLExecutionResultHandler} instance
     * @param responseSender                the {@link GraphQLWsSender} instance
     */
    public GraphQLWsMessageHandler(
        final GraphQLWsConfiguration graphQLWsConfiguration,
        final GraphQLWsState state,
        final GraphQLInvocation invocation,
        final GraphQLExecutionResultHandler graphQLExecutionResultHandler,
        final GraphQLWsSender responseSender,
        final List<GraphQLWsFilter> filters) {
        this.graphQLWsConfiguration = graphQLWsConfiguration;
        this.state = state;
        this.graphQLInvocation = invocation;
        this.graphQLExecutionResultHandler = graphQLExecutionResultHandler;
        this.responseSender = responseSender;
        this.filters = new ArrayList<>(filters);
        this.filters.add(this.createMessageHandler());
    }

    /**
     * Handles the request possibly invocating graphql.
     *
     * @param request Message from client
     * @param session WebSocketSession
     * @return publisher of graphql subscription responses
     */
    public Publisher<GraphQLWsResponse> handleMessage(final GraphQLWsRequest request, final WebSocketSession session) {
        return new GraphQLWsFilterChain(this.filters).proceed(request, session);
    }

    private GraphQLWsFilter createMessageHandler() {
        return (request, session, chain) -> {
            switch (request.getType()) {
                case GQL_CONNECTION_INIT:
                    return GraphQLWsMessageHandler.this.init(session);
                case GQL_START:
                    return GraphQLWsMessageHandler.this.startOperation(request, session);
                case GQL_STOP:
                    return GraphQLWsMessageHandler.this.state.stopOperation(request, session);
                case GQL_CONNECTION_TERMINATE:
                    return GraphQLWsMessageHandler.this.state.terminateSession(session);
                default:
                    throw new IllegalStateException("Unexpected value: " + request.getType());
            }
        };
    }

    private Publisher<GraphQLWsResponse> init(final WebSocketSession session) {
        if (this.graphQLWsConfiguration.isKeepAliveEnabled()) {
            this.state.activateSession(session);
            return Flowable.just(new GraphQLWsResponse(GQL_CONNECTION_ACK),
                new GraphQLWsResponse(GQL_CONNECTION_KEEP_ALIVE));
        } else {
            return Flowable.just(new GraphQLWsResponse(GQL_CONNECTION_ACK));
        }
    }

    private Publisher<GraphQLWsResponse> startOperation(final GraphQLWsRequest request,
        final WebSocketSession session) {
        if (request.getId() == null) {
            log.warn("GraphQL operation id is required with type start");
            return Flowable.just(new GraphQLWsResponse(GQL_ERROR));
        }

        if (this.state.operationExists(request, session)) {
            log.info("Already subscribed to operation {} in session {}", request.getId(), session.getId());
            return Flowable.empty();
        }

        final GraphQLRequestBody payload = request.getPayload();
        if (payload == null || StringUtils.isEmpty(payload.getQuery())) {
            log.info("Payload was null or query empty for operation {} in session {}", request.getId(),
                session.getId());
            return Flowable.just(new GraphQLWsResponse(GQL_ERROR, request.getId()));
        }

        return this.executeRequest(request.getId(), payload, session);
    }

    private Publisher<GraphQLWsResponse> executeRequest(final String operationId, final GraphQLRequestBody payload,
        final WebSocketSession session) {
        final GraphQLInvocationData invocationData =
            new GraphQLInvocationData(payload.getQuery(), payload.getOperationName(), payload.getVariables());
        final HttpRequest<?> httpRequest = session.get(HTTP_REQUEST_KEY, HttpRequest.class)
            .orElseThrow(() -> new RuntimeException("HttpRequest could not be retrieved from websocket session"));
        final Publisher<ExecutionResult> executionResult =
            this.graphQLInvocation.invoke(invocationData, httpRequest, null);
        final Publisher<GraphQLResponseBody> responseBody =
            this.graphQLExecutionResultHandler.handleExecutionResult(executionResult);
        return Flowable.fromPublisher(responseBody)
            .flatMap(body -> this.responseSender.send(operationId, body, session));
    }
}
