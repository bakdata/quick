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
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.List;

/**
 * Data Fetcher that takes the query's key, rangeFrom, and rangeTo arguments and fetches values from the mirror's range
 * index.
 */
public class RangeQueryFetcher<T> implements DataFetcher<List<T>> {
    private final String argument;
    private final String rangeFrom;
    private final String rangeTo;
    private final DataFetcherClient<T> dataFetcherClient;
    private final boolean isNullable;

    /**
     * Standard constructor.
     *
     * @param argument name of the argument to extract key from
     * @param dataFetcherClient underlying HTTP mirror client
     * @param rangeFrom name of the range from field
     * @param rangeTo name of the range to field
     * @param isNullable true if list itself can be null
     */
    public RangeQueryFetcher(final String argument,
        final DataFetcherClient<T> dataFetcherClient,
        final String rangeFrom, final String rangeTo, final boolean isNullable) {
        this.dataFetcherClient = dataFetcherClient;
        this.argument = argument;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.isNullable = isNullable;
    }

    @Override
    @Nullable
    public List<T> get(final DataFetchingEnvironment environment) {
        final Object argumentValue = DeferFetcher.getArgument(this.argument, environment)
            .orElseThrow(() -> new RuntimeException("Could not find argument " + this.argument));
        final Object rangeFromValue = DeferFetcher.getArgument(this.rangeFrom, environment)
            .orElseThrow(() -> new RuntimeException("Could not find argument " + this.rangeFrom));
        final Object rangeToValue = DeferFetcher.getArgument(this.rangeTo, environment)
            .orElseThrow(() -> new RuntimeException("Could not find argument " + this.rangeTo));

        final List<T> results = this.dataFetcherClient.fetchRange(argumentValue.toString(), rangeFromValue.toString(),
            rangeToValue.toString());

        // got null but schema doesn't allow null
        // semantically, there is no difference between null and an empty list for us in this case
        // we therefore continue gracefully by simply returning a list and not throwing an exception
        if (results == null && !this.isNullable) {
            return Collections.emptyList();
        }

        return results;
    }
}
