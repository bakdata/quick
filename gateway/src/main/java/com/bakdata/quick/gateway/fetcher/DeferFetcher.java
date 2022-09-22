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

package com.bakdata.quick.gateway.fetcher;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.Optional;

/**
 * A data fetcher for adding the query's arguments to the local context for use in downstream fetchers.
 *
 * <p>
 * Consider the following definition:
 * <pre>
 *     type Query {
 *         getPurchaseInfo(id: ID): PurchaseInfo
 *     }
 *
 *     type PurchaseInfo {
 *         purchase: Purchase
 *         purchaseStats: PurchaseStatistics
 *     }
 *
 *     ...
 * </pre>
 *
 * <p>
 * In order to resolve the purchase and purchaseStats fields, a {@link QueryKeyArgumentFetcher} is used. This fetcher
 * requires the value of the id argument from the `getPurchaseInfo` query. However, the argument is not propagated in
 * the respective {@link DataFetchingEnvironment} because a {@link graphql.schema.PropertyDataFetcher} is used by
 * default for Query:getPurchaseInfo.
 *
 * <p>
 * This class can be used as a replacement in such situations. It adds the arguments to the environment's local
 * context.
 */
public class DeferFetcher<T> implements DataFetcher<DataFetcherResult<Object>> {
    /**
     * A dummy value as a placeholder required for deferring the query to all children.
     */
    private static final Object PLACEHOLDER = new Object();

    /**
     * Retrieves the value of an argument from the user query.
     *
     * <p>
     * With this, arguments passed through with this class are handled correctly.
     *
     * @param argument name of the argument to extract value from
     * @param environment current data fetching environment
     * @return object if argument was found, empty otherwise
     */
    public Optional<T> getArgument(final String argument, final DataFetchingEnvironment environment) {
        if (environment.containsArgument(argument)) {
            return Optional.ofNullable(environment.getArgument(argument));
        } else if (environment.getLocalContext() instanceof Map) {
            final Map<String, T> localContext = environment.getLocalContext();
            return Optional.ofNullable(localContext.get(argument));
        }
        return Optional.empty();
    }

    @Override
    public DataFetcherResult<Object> get(final DataFetchingEnvironment environment) {
        return DataFetcherResult.newResult()
            .data(PLACEHOLDER) // DO NOT REMOVE! if data is null, the whole query will always return null!
            .localContext(environment.getArguments())
            .build();
    }
}
