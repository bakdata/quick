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

import graphql.ExecutionResult;
import io.micronaut.configuration.graphql.DefaultGraphQLInvocation;
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer;
import io.micronaut.configuration.graphql.GraphQLInvocation;
import io.micronaut.configuration.graphql.GraphQLInvocationData;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import jakarta.inject.Singleton;
import org.dataloader.DataLoaderRegistry;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;

/**
 * Indirection for {@link DefaultGraphQLInvocation} that allows updating the underlying {@link graphql.GraphQL}.
 */
@Singleton
@Replaces(DefaultGraphQLInvocation.class)
public class QuickGraphQLInvocation implements GraphQLInvocation {
    private DefaultGraphQLInvocation invocation;

    /**
     * Default constructor.
     *
     * @param graphQLExecutionInputCustomizer the {@link GraphQLExecutionInputCustomizer} instance
     * @param dataLoaderRegistry              the {@link DataLoaderRegistry} instance
     * @param graphQLContext                  the {@link graphql.GraphQLContext} instance
     */
    public QuickGraphQLInvocation(
        final GraphQLExecutionInputCustomizer graphQLExecutionInputCustomizer,
        @Nullable final BeanProvider<DataLoaderRegistry> dataLoaderRegistry,
        final QuickGraphQLContext graphQLContext) {
        this.invocation = new DefaultGraphQLInvocation(
            graphQLContext.getGraphQL(),
            graphQLExecutionInputCustomizer,
            dataLoaderRegistry
        );

        graphQLContext.addGraphQLUpdateCallback(context ->
            this.invocation = new DefaultGraphQLInvocation(
                context.getGraphQL(),
                graphQLExecutionInputCustomizer,
                dataLoaderRegistry
            )
        );
    }

    @Override
    public Publisher<ExecutionResult> invoke(final GraphQLInvocationData invocationData, final HttpRequest httpRequest,
        @Nullable final MutableHttpResponse<String> httpResponse) {
        return this.invocation.invoke(invocationData, httpRequest, httpResponse);
    }

}
