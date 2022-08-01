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

import com.bakdata.quick.common.security.SecurityConfig;
import com.bakdata.quick.gateway.subscriptions.GraphQLWsRequest.ClientType;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.configuration.graphql.ws.GraphQLWsResponse.ServerType;
import io.micronaut.websocket.WebSocketSession;
import io.reactivex.Flowable;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.reactivestreams.Publisher;

/**
 * Hook for authenticating subscriptions.
 *
 * <p>
 * The GraphQL protocol specifies that authentication should happen during the connection init. This hook processes
 * {@link ClientType#GQL_CONNECTION_INIT} requests and checks whether the correct 'X-API-Key* is sent as payload.
 * </p>
 */
@Singleton
public class GraphQLWsAuthFilter implements GraphQLWsFilter {

    private final SecurityConfig securityConfig;
    private final GraphQLWsState state;

    public GraphQLWsAuthFilter(final SecurityConfig securityConfig,
        final GraphQLWsState state) {
        this.securityConfig = securityConfig;
        this.state = state;
    }

    @Override
    public Publisher<GraphQLWsResponse> doFilter(final GraphQLWsRequest request, final WebSocketSession session,
        final GraphQLWsFilterChain chain) {
        if (!this.securityConfig.isSecurityEnabled() || request.getType() != ClientType.GQL_CONNECTION_INIT) {
            return chain.proceed(request, session);
        }

        final GraphQLRequestBody payload = request.getPayload();

        // Authenticated -> no response is generated and the request is processed downstream
        if (payload != null && this.securityConfig.getApiKey().equals(payload.getAuthToken())) {
            return chain.proceed(request, session);
        }

        // otherwise, we can immediately send a error response
        final GraphQLResponseBody responseBody = new GraphQLResponseBody(
            // error -> list of errors to align with GraphQL's error specifications
            // with that, clients should display the error properly
            Map.of("errors", List.of(new GraphQLUnauthorizedError())));
        final GraphQLWsResponse response = new GraphQLWsResponse(ServerType.GQL_ERROR, request.getId(), responseBody);
        this.state.terminateSession(session);
        return Flowable.just(response);
    }

    @Override
    public int getOrder() {
        return 5;
    }

    /**
     * POJO for unauthorized subscription access.
     */
    @Data
    public static class GraphQLUnauthorizedError {
        private String message = "Unauthorized";
        private List<String> path = List.of("connectionParams", "X-API-Key");
    }
}
