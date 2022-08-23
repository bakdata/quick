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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.bakdata.quick.gateway.GraphQLWsClient.SerializableRequest;
import com.bakdata.quick.gateway.subscriptions.GraphQLRequestBody;
import com.bakdata.quick.gateway.subscriptions.GraphQLWsAuthFilter.GraphQLUnauthorizedError;
import com.bakdata.quick.gateway.subscriptions.GraphQLWsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.rxjava2.http.client.websockets.RxWebSocketClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

@Property(name = "micronaut.security.enabled", value = StringUtils.TRUE)
@MicronautTest
class GraphQLSecurityTest {

    @Property(name = "quick.apikey")
    String key;

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/")
    RxHttpClient client;

    @Inject
    @Client("/")
    RxWebSocketClient websocketClient;

    @Test
    void shouldSecureGraphQLEndpoint() {
        final HttpRequest<?> request = HttpRequest
            .create(HttpMethod.POST, "/graphql")
            .contentType(MediaType.APPLICATION_GRAPHQL_TYPE)
            .header("X-API-Key", "no")
            .body("{test(id: 123)}");

        final BlockingHttpClient httpClient = this.client.toBlocking();

        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> httpClient.exchange(request))
            .withMessage("Client '/': Unauthorized");
    }

    @Test
    void shouldSecureGraphQLWsEndpoint() {
        final GraphQLWsClient client =
            this.websocketClient.connect(GraphQLWsClient.class, "/graphql-ws").blockingFirst();
        final GraphQLWsRequest request = new GraphQLWsRequest();
        request.setType(GraphQLWsRequest.ClientType.GQL_CONNECTION_INIT.getType());
        client.send(new SerializableRequest(request));

        final GraphQLWsResponse graphQLWsResponse = client.nextResponse();
        assertThat(graphQLWsResponse)
            .satisfies(response -> {
                assertThat(response.getType()).isEqualTo(ServerType.GQL_ERROR.getType());
            })
            .extracting(GraphQLWsResponse::getPayload)
            .extracting(GraphQLResponseBody::getSpecification)
            .asInstanceOf(MAP)
            .extractingByKey("errors")
            .asInstanceOf(InstanceOfAssertFactories.list(Map.class))
            .hasSize(1)
            .first()
            .satisfies(exception -> {
                final ObjectMapper objectMapper = new ObjectMapper();
                final GraphQLUnauthorizedError ex =
                    objectMapper.convertValue(exception, GraphQLUnauthorizedError.class);
                assertThat(ex).isEqualTo(new GraphQLUnauthorizedError());
            });

        final GraphQLWsResponse response = client.nextResponse();
        assertThat(response).isNull();
    }

    @Test
    void shouldAllowAuthenticatedSubscriptionRequest() {
        final GraphQLWsClient client =
            this.websocketClient.connect(GraphQLWsClient.class, "/graphql-ws").blockingFirst();
        final GraphQLWsRequest request = new GraphQLWsRequest();
        request.setType(GraphQLWsRequest.ClientType.GQL_CONNECTION_INIT.getType());
        final GraphQLRequestBody payload = new GraphQLRequestBody();
        payload.setAuthToken("test_key");
        request.setPayload(payload);
        client.send(new SerializableRequest(request));

        final GraphQLWsResponse graphQLWsResponse = client.nextResponse();
        assertThat(graphQLWsResponse.getType())
            .isEqualTo(ServerType.GQL_CONNECTION_ACK.getType());
    }
}
