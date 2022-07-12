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


import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_COMPLETE;

import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Keeps the state of the web socket subscriptions.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
@Slf4j
class GraphQLWsState {

    private final ConcurrentSkipListSet<String> activeSessions = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<String, GraphQLWsOperations> activeOperations = new ConcurrentHashMap<>();

    /**
     * Sets the session to active.
     *
     * @param session WebSocketSession
     */
    void activateSession(final WebSocketSession session) {
        log.debug("Activate session {}", session.getId());
        this.activeSessions.add(session.getId());
    }

    /**
     * Whether the session is considered active, which means the client called init but not yet terminate.
     *
     * @param session WebSocketSession
     * @return whether the session is active
     */
    boolean isActive(final WebSocketSession session) {
        return this.activeSessions.contains(session.getId());
    }

    /**
     * Sets the GraphQLWsOperations for the client.
     *
     * @param session WebSocketSession
     */
    void init(final WebSocketSession session) {
        log.debug("Init session {}", session.getId());
        this.activeOperations.putIfAbsent(session.getId(), new GraphQLWsOperations());
    }

    /**
     * Stop and remove all subscriptions for the session.
     *
     * @param session WebSocketSession
     * @return publisher of graphql subscription responses
     */
    Publisher<GraphQLWsResponse> terminateSession(final WebSocketSession session) {
        log.debug("Terminate session {}", session.getId());
        this.activeSessions.remove(session.getId());
        Optional.ofNullable(this.activeOperations.remove(session.getId()))
            .ifPresent(GraphQLWsOperations::cancelAll);
        return Flowable.empty();
    }

    /**
     * Saves the operation under the client.id and operation.id so it can be cancelled later.
     *
     * @param operationId String
     * @param session     WebSocketSession
     * @param starter     Function to start the subscription, will only be called if not already present
     */
    void saveOperation(final String operationId, final WebSocketSession session,
        final Function<String, Subscription> starter) {
        Optional.ofNullable(session)
            .map(WebSocketSession::getId)
            .map(this.activeOperations::get)
            .ifPresent(graphQLWsOperations -> graphQLWsOperations.addSubscription(operationId, starter));
    }

    /**
     * Stops the current operation is present and returns the proper message.
     *
     * @param request GraphQLWsRequest
     * @param session WebSocketSession
     * @return the complete message, or nothing if there was no operation running
     */
    Publisher<GraphQLWsResponse> stopOperation(final GraphQLWsRequest request, final WebSocketSession session) {
        final String sessionId = session.getId();
        final String operationId = request.getId();
        if (operationId == null || sessionId == null) {
            return Flowable.empty();
        }
        final boolean removed = Optional.ofNullable(this.activeOperations.get(sessionId))
            .map(graphQLWsOperations -> {
                graphQLWsOperations.cancelOperation(operationId);
                return graphQLWsOperations.removeCompleted(operationId);
            }).orElse(false);
        return removed ? Flowable.just(new GraphQLWsResponse(GQL_COMPLETE, operationId)) : Flowable.empty();
    }

    /**
     * Remove the operation once completed, to clean up and prevent sending a second complete on stop.
     *
     * @param operationId String
     * @param session     WebSocketSession
     * @return whether the operation was removed
     */
    boolean removeCompleted(final String operationId, final WebSocketSession session) {
        return Optional.ofNullable(session)
            .map(WebSocketSession::getId)
            .map(this.activeOperations::get)
            .map(graphQLWsOperations -> graphQLWsOperations.removeCompleted(operationId))
            .orElse(false);
    }

    /**
     * Returns whether the operation already exists.
     *
     * @param request GraphQLWsRequest
     * @param session WebSocketSession
     * @return true or false
     */
    boolean operationExists(final GraphQLWsRequest request, final WebSocketSession session) {
        return Optional.ofNullable(session)
            .map(WebSocketSession::getId)
            .map(this.activeOperations::get)
            .map(graphQLWsOperations -> graphQLWsOperations.operationExists(request))
            .orElse(false);
    }
}
