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
import io.micronaut.websocket.WebSocketSession;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;

/**
 * A filter chain for intercepting GraphQL subscription messages.
 */
public class GraphQLWsFilterChain {
    private final List<GraphQLWsFilter> filters;
    private final int length;
    private final AtomicInteger position;

    /**
     * Constructor.
     *
     * @param filters all filters to apply in this chain
     */
    public GraphQLWsFilterChain(final List<GraphQLWsFilter> filters) {
        this.filters = filters;
        this.length = filters.size();
        this.position = new AtomicInteger();
    }

    /**
     * Proceeds to the next interceptor or final request invocation.
     */
    public Publisher<GraphQLWsResponse> proceed(final GraphQLWsRequest request, final WebSocketSession session) {
        final int pos = this.position.getAndIncrement();
        if (pos > this.length) {
            throw new IllegalStateException(
                "The FilterChain.proceed(..) method should be invoked exactly once per filter execution. The "
                    + "method has instead been invoked multiple times by an erroneous filter definition.");
        } else {
            final GraphQLWsFilter filter = this.filters.get(pos);
            return filter.doFilter(request, session, this);
        }
    }
}
