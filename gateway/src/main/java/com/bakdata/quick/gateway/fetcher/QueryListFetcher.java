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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Data Fetcher that fetches all values in  a mirror's key value store.
 *
 * <p>
 * Consider the following schema:
 * <pre>
 *   type Query {
 *       allPurchases: [Purchase]
 *   }
 * </pre>
 *
 * <p>
 * There, the gateway must fetch all purchases from the corresponding mirror as there is no argument. This is done by
 * this data fetcher.
 */
public class QueryListFetcher<T> implements DataFetcher<List<T>> {
    private final DataFetcherClient<T> dataFetcherClient;
    private final boolean isNullable;
    private final boolean hasNullableElements;

    /**
     * Standard constructor.
     *
     * @param dataFetcherClient   mirror http client
     * @param isNullable          true if list itself can be null
     * @param hasNullableElements true if list elements can be null
     */
    public QueryListFetcher(final DataFetcherClient<T> dataFetcherClient, final boolean isNullable,
        final boolean hasNullableElements) {
        this.dataFetcherClient = dataFetcherClient;
        this.isNullable = isNullable;
        this.hasNullableElements = hasNullableElements;
    }

    @Override
    @Nullable
    public List<T> get(final DataFetchingEnvironment environment) {
        final List<T> values = this.dataFetcherClient.fetchList();

        // got null but schema doesn't allow null
        // semantically, there is no difference between null and an empty list for us in this case
        // we therefore continue gracefully by simply returning a list and  not throwing an exception
        if (values == null && !this.isNullable) {
            return Collections.emptyList();
        }

        // null elements are not allowed, so we have to filter them
        if (values != null && !this.hasNullableElements) {
            return values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }

        return values;
    }
}
