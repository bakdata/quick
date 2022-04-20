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

package com.bakdata.quick.gateway;

import com.bakdata.quick.gateway.subscriptions.GraphQLRequestBody;
import com.bakdata.quick.gateway.subscriptions.GraphQLWsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.JacksonGraphQLJsonSerializer;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnMessage;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.Data;

/**
 * Simple implementation for GraphQL WS client support Taken from: Micronaut https://github
 * .com/micronaut-projects/micronaut-graphql/blob/master/graphql/src/test/groovy/io/micronaut/configuration/graphql
 * /ws/GraphQLWsClient.groovy
 */
@ClientWebSocket(uri = "/graphql-ws", subprotocol = "graphql-ws")
public abstract class GraphQLWsClient implements AutoCloseable {

    private final BlockingQueue<GraphQLWsResponse> responses = new ArrayBlockingQueue<>(10);
    private final GraphQLJsonSerializer serializer;
    private WebSocketSession session;

    public GraphQLWsClient() {
        this.serializer = new JacksonGraphQLJsonSerializer(new ObjectMapper());
    }

    @Override
    public void close() throws Exception {
        this.session.close();
    }

    @OnMessage
    void onMessage(final String message, final WebSocketSession session) {
        this.session = session;
        final DeserializableResponse response = this.serializer.deserialize(message, DeserializableResponse.class);
        this.responses.add(new GraphQLWsResponse(response.getType(), response.id, response.payload));
    }

    abstract void send(SerializableRequest message);

    GraphQLWsResponse nextResponse() {
        final GraphQLWsResponse response;
        try {
            response = this.responses.poll(1, TimeUnit.SECONDS);
            return response;
        } catch (final InterruptedException e) {
            throw new RuntimeException("");
        }
    }

    @Data
    static class DeserializableResponse {

        String type;
        @Nullable
        String id;
        @Nullable
        GraphQLResponseBody payload;

        public DeserializableResponse() {
        }

        DeserializableResponse(final String type, final String id, final GraphQLResponseBody payload) {
            this.type = type;
            this.id = id;
            this.payload = payload;
        }

        ServerType getType() {
            for (final GraphQLWsResponse.ServerType serverType : GraphQLWsResponse.ServerType.values()) {
                if (serverType.getType().equals(this.type)) {
                    return serverType;
                }
            }
            throw new RuntimeException();
        }

        void setType(final String type) {
            this.type = type;
        }

        void setId(final String id) {
            this.id = id;
        }

        void setPayload(final GraphQLResponseBody payload) {
            this.payload = payload;
        }
    }

    @Data
    static class SerializableRequest {

        String type;
        @Nullable
        String id;
        @Nullable
        GraphQLRequestBody payload;

        SerializableRequest(final GraphQLWsRequest request) {
            this.type = request.getType().getType();
            this.id = request.getId();
            this.payload = request.getPayload();
        }
    }
}
