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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Data Fetcher that fetchers a list of values in a mirror's key value store.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * type Query {
 *  findPurchases(purchaseId: [ID]): [Purchase] @topic(name: "purchase-topic", keyArgument: "purchaseId") # <- query list fetcher
 * }
 *
 * type Purchase  {
 *  purchaseId: ID!,
 *  productId: ID!,
 * }
 * }</pre>
 *
 * <p>
 * The gateway receives a list of purchase-IDs and sends them to the mirror and should receive a list of purchases.
 */
public class ListArgumentFetcher<V> implements DataFetcher<List<V>> {
    private final String argument;
    private final DataFetcherClient<V> dataFetcherClient;
    private final boolean isNullable;
    private final boolean hasNullableElements;

    /**
     * Standard constructor.
     *
     * @param argument an argument of type list containing the keys
     * @param dataFetcherClient http client for mirror
     * @param isNullable true if field that is being fetched can be null
     */
    public ListArgumentFetcher(final String argument,
        final DataFetcherClient<V> dataFetcherClient,
        final boolean isNullable, final boolean hasNullableElements) {
        this.argument = argument;
        this.dataFetcherClient = dataFetcherClient;
        this.isNullable = isNullable;
        this.hasNullableElements = hasNullableElements;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public List<V> get(final DataFetchingEnvironment environment) {
        final Object arguments = DeferFetcher.getArgument(this.argument, environment)
            .orElseThrow(() -> new RuntimeException("Could not find argument " + this.argument));

        List<V> results = null;
        if (arguments instanceof List) {
            final List<String> stringArgument =
                ((Collection<?>) arguments).stream().map(Object::toString).collect(Collectors.toList());
            results = this.dataFetcherClient.fetchResults(stringArgument);
        }

        // got null but schema doesn't allow null
        // semantically, there is no difference between null and an empty list for us in this case
        // we therefore continue gracefully by simply returning a list and not throwing an exception
        if (results == null && !this.isNullable) {
            return Collections.emptyList();
        }

        // null elements are not allowed, so we have to filter them
        if (results != null && !this.hasNullableElements) {
            return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }

        return results;
    }
}
