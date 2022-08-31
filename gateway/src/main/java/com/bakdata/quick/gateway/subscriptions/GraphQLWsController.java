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

import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_CONNECTION_ERROR;

import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.configuration.graphql.ws.GraphQLWsConfiguration;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * The GraphQL websocket controller handling GraphQL requests. Implementation of https://github
 * .com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Replaces(io.micronaut.configuration.graphql.ws.GraphQLWsController.class)
@Slf4j
@ServerWebSocket(value =
    "${" + GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.PATH + ":"
        + GraphQLWsConfiguration.DEFAULT_PATH + "}", subprotocols = "graphql-ws")
@Requires(property = GraphQLWsConfiguration.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class GraphQLWsController {

    public static final String HTTP_REQUEST_KEY = "httpRequest";

    private final GraphQLWsMessageHandler messageHandler;
    private final GraphQLWsState state;
    private final GraphQLJsonSerializer graphQLJsonSerializer;
    private final GraphQLWsResponse errorMessage;

    /**
     * Default constructor.
     *
     * @param messageHandler        the {@link GraphQLWsMessageHandler} instance
     * @param state                 the {@link GraphQLWsState} instance
     * @param graphQLJsonSerializer the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLWsController(final GraphQLWsMessageHandler messageHandler, final GraphQLWsState state,
                               final GraphQLJsonSerializer graphQLJsonSerializer) {
        this.messageHandler = messageHandler;
        this.state = state;
        this.graphQLJsonSerializer = graphQLJsonSerializer;
        this.errorMessage = new GraphQLWsResponse(GQL_CONNECTION_ERROR);
    }

    /**
     * Called when the connection is opened. We store the original request, since it might be needed for the
     * GraphQLInvocation.
     *
     * @param session WebSocketSession
     * @param request HttpRequest
     */
    @OnOpen
    public void onOpen(final WebSocketSession session, final HttpRequest<?> request) {
        session.put(HTTP_REQUEST_KEY, request);
        this.state.init(session);
        log.trace("Opened websocket connection with id {}", session.getId());
    }


    /**
     * Called on every message received from the client.
     *
     * @param message Message received from a client
     * @param session WebSocketSession
     * @return publisher of websocket responses
     */
    @OnMessage
    public Publisher<GraphQLWsResponse> onMessage(final String message, final WebSocketSession session) {
        try {
            final GraphQLWsRequest request = this.graphQLJsonSerializer.deserialize(message, GraphQLWsRequest.class);
            if (request.getType() == null) {
                log.warn("Type was null on operation message");
                return this.send(Flowable.just(this.errorMessage), session);
            } else {
                return this.send(this.messageHandler.handleMessage(request, session), session);
            }
        } catch (final RuntimeException e) {
            log.warn("Error deserializing message received from client: {}", message, e);
            return this.send(Flowable.just(this.errorMessage), session);
        }
    }

    /**
     * Called when the websocket is closed.
     *
     * @param session     WebSocketSession
     * @param closeReason CloseReason
     * @return publisher of websocket responses
     */
    @OnClose
    public Publisher<GraphQLWsResponse> onClose(final WebSocketSession session, final CloseReason closeReason) {
        log.trace("Closed websocket connection with id {} with reason {}", session.getId(), closeReason);
        return this.send(this.state.terminateSession(session), session);
    }

    /**
     * Called when there is an error with the websocket, this probably means the connection is lost, but hasn't been
     * properly closed.
     *
     * @param session WebSocketSession
     * @param t       Throwable, the cause of the error
     * @return publisher of websocket responses
     */
    @OnError
    public Publisher<GraphQLWsResponse> onError(final WebSocketSession session, final Throwable t) {
        log.debug("Error websocket connection with id {} with error {}", session.getId(), t.getMessage());
        return this.send(this.state.terminateSession(session), session);
    }

    private Publisher<GraphQLWsResponse> send(final Publisher<GraphQLWsResponse> publisher,
                                              final WebSocketSession session) {
        return Publishers.then(publisher, response -> {
            if (session.isOpen()) {
                session.sendSync(this.graphQLJsonSerializer.serialize(response));
            }
        });
    }
}
