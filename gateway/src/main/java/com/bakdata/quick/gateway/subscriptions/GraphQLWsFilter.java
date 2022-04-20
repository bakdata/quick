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

package com.bakdata.quick.gateway.subscriptions;

import io.micronaut.configuration.graphql.ws.GraphQLWsResponse;
import io.micronaut.core.order.Ordered;
import io.micronaut.websocket.WebSocketSession;
import org.reactivestreams.Publisher;

/**
 * Filter for mutating GraphQL subscription messages.
 *
 * @see io.micronaut.http.filter.HttpFilter
 */
public interface GraphQLWsFilter extends Ordered {
    /**
     * Intercepts a {@link GraphQLWsRequest}.
     *
     * @param request the intercepted {@link GraphQLWsRequest} instance
     * @param session the {@link WebSocketSession} the request stems from
     * @param chain   the {@link GraphQLWsFilterChain} instance
     * @return a {@link Publisher} for the response
     */
    Publisher<GraphQLWsResponse> doFilter(final GraphQLWsRequest request, final WebSocketSession session,
        GraphQLWsFilterChain chain);
}
