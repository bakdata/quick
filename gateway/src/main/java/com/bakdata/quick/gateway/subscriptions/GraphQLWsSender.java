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
import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_DATA;
import static io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType.GQL_ERROR;

import graphql.ExecutionResult;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.core.async.subscriber.CompletionAwareSubscriber;
import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import java.util.Collection;
import java.util.function.Function;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Sends the GraphQL response(s) to the client.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
@Slf4j
public class GraphQLWsSender {


    private final GraphQLWsState state;
    private final GraphQLJsonSerializer graphQLJsonSerializer;

    /**
     * Default constructor.
     *
     * @param state                 the {@link GraphQLWsState} instance
     * @param graphQLJsonSerializer the {@link GraphQLJsonSerializer} instance
     */
    public GraphQLWsSender(final GraphQLWsState state, final GraphQLJsonSerializer graphQLJsonSerializer) {
        this.state = state;
        this.graphQLJsonSerializer = graphQLJsonSerializer;
    }

    /**
     * Transform the result from the a websocket request to a message that can be send to the client.
     *
     * @param operationId  Sting value of the operation id
     * @param responseBody GraphQLResponseBody of the executed operation
     * @param session      the websocket session by which the operation was executed
     * @return GraphQLWsOperationMessage
     */
    @SuppressWarnings("unchecked")
    Flowable<GraphQLWsResponse> send(final String operationId, final GraphQLResponseBody responseBody,
        final WebSocketSession session) {
        final Object dataObject = responseBody.getSpecification().get("data");
        if (dataObject instanceof Publisher) {
            return this.startSubscription(operationId, (Publisher<ExecutionResult>) dataObject, session);
        } else {
            return Flowable.just(
                this.toGraphQLWsResponse(operationId, responseBody),
                new GraphQLWsResponse(GQL_COMPLETE, operationId));
        }
    }

    private GraphQLWsResponse toGraphQLWsResponse(final String operationId, final GraphQLResponseBody responseBody) {
        if (this.hasErrors(responseBody)) {
            return new GraphQLWsResponse(GQL_ERROR, operationId, responseBody);
        } else {
            return new GraphQLWsResponse(GQL_DATA, operationId, responseBody);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean hasErrors(final GraphQLResponseBody responseBody) {
        final Object errorObject = responseBody.getSpecification().get("errors");
        if (errorObject instanceof Collection) {
            return !((Collection) errorObject).isEmpty();
        } else {
            return false;
        }
    }

    private Function<String, Subscription> starter(final Publisher<ExecutionResult> publisher,
        final WebSocketSession session) {
        return operationId -> {
            final SendSubscriber subscriber = new SendSubscriber(operationId, session);
            publisher.subscribe(subscriber);
            return subscriber.getSubscription();
        };
    }

    private Flowable<GraphQLWsResponse> startSubscription(final String operationId,
        final Publisher<ExecutionResult> publisher,
        final WebSocketSession session) {
        this.state.saveOperation(operationId, session, this.starter(publisher, session));
        return Flowable.empty();
    }

    /**
     * Subscriber to handle the messages, might be cancelled when the client calls stop or when the connection is
     * broken.
     */
    private final class SendSubscriber extends CompletionAwareSubscriber<ExecutionResult> {

        private final String operationId;
        private final WebSocketSession session;

        private SendSubscriber(final String operationId, final WebSocketSession session) {
            this.operationId = operationId;
            this.session = session;
        }

        Subscription getSubscription() {
            return this.subscription;
        }

        @Override
        protected void doOnSubscribe(final Subscription subscription) {
            log.info("Subscribed to results for to operation {} in session {}", this.operationId, this.session.getId());
            subscription.request(1L);
        }

        @Override
        protected void doOnNext(final ExecutionResult message) {
            log.debug("Emit output for {}", this.operationId);
            this.convertAndSend(message);
            this.subscription.request(1L);
        }

        @Override
        protected void doOnError(final Throwable t) {
            log.warn("Error in SendSubscriber", t);
            this.send(new GraphQLWsResponse(GQL_ERROR, this.operationId));
        }

        @Override
        protected void doOnComplete() {
            log.info("Completed results for operation {} in session {}", this.operationId, this.session.getId());
            if (GraphQLWsSender.this.state.removeCompleted(this.operationId, this.session)) {
                this.send(new GraphQLWsResponse(GQL_COMPLETE, this.operationId));
            }
        }

        private void convertAndSend(final ExecutionResult executionResult) {
            final GraphQLWsResponse response = GraphQLWsSender.this.toGraphQLWsResponse(
                this.operationId, new GraphQLResponseBody(executionResult.toSpecification()));
            this.send(response);
        }

        private void send(final GraphQLWsResponse response) {
            if (this.session.isOpen()) {
                this.session.sendSync(GraphQLWsSender.this.graphQLJsonSerializer.serialize(response));
            }
        }
    }
}
