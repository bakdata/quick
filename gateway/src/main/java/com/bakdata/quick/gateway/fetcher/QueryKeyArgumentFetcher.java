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

import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;


/**
 * Data Fetcher that takes the query's argument and fetches values by sending a request to the given address.
 */
public class QueryKeyArgumentFetcher<T> implements DataFetcher<T> {
    private final String argument;
    private final DataFetcherClient<T> dataFetcherClient;
    private final boolean isNullable;

    /**
     * Standard constructor.
     *
     * @param argument          name of the argument containing the key
     * @param dataFetcherClient http client for mirror
     * @param isNullable        true if field that is being fetched can be null
     */
    public QueryKeyArgumentFetcher(final String argument, final DataFetcherClient<T> dataFetcherClient,
        final boolean isNullable) {
        this.argument = argument;
        this.dataFetcherClient = dataFetcherClient;
        this.isNullable = isNullable;
    }

    @Override
    @Nullable
    public T get(final DataFetchingEnvironment environment) {
        final Object argumentValue = DeferFetcher.getArgument(this.argument, environment)
            .orElseThrow(() -> new RuntimeException("Could not find argument " + this.argument));
        final T value = this.dataFetcherClient.fetchResult(argumentValue.toString());
        if (value == null && !this.isNullable) {
            throw new NonNullableFieldWasNullException(environment.getExecutionStepInfo(),
                environment.getExecutionStepInfo().getPath());
        }
        return value;
    }

}
