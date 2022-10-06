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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.TestModels.Product;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RangeQueryFetcherTest {
    @Test
    void shouldFetchRangeOfObjectsWithKeyInteger() {
        final Product product1 = Product.builder()
            .productId(1)
            .name("productTest1")
            .ratings(4)
            .build();

        final Product product2 = Product.builder()
            .productId(2)
            .name("productTest2")
            .ratings(3)
            .build();

        final List<Product> userRequests = List.of(product1, product2);

        final PartitionedMirrorClient<Integer, Product> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchRange(eq(1), eq("1"), eq("3"))).thenReturn(userRequests);
        final DataFetcherClient<Integer, Product> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final RangeQueryFetcher<Integer, Product> rangeQueryFetcher =
            new RangeQueryFetcher<>("productId", fetcherClient, "ratingFrom", "ratingTo", true);

        final Map<String, Object> arguments = Map.of("productId", 1, "ratingFrom", 1, "ratingTo", 4);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Product> actual = rangeQueryFetcher.get(env);

        verify(partitionedMirrorClient).fetchRange(eq(1), eq("1"), eq("3"));
        assertThat(actual).isEqualTo(userRequests);
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() {
        final PartitionedMirrorClient<Integer, Product> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchRange(eq(9), eq("6"), eq("9"))).thenReturn(Collections.emptyList());
        final DataFetcherClient<Integer, Product> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final RangeQueryFetcher<Integer, Product> rangeQueryFetcher =
            new RangeQueryFetcher<>("productId", fetcherClient, "ratingFrom", "ratingTo", false);

        final Map<String, Object> arguments = Map.of("productId", 9, "ratingFrom", 6, "ratingTo", 10);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Product> actual = rangeQueryFetcher.get(env);

        verify(partitionedMirrorClient).fetchRange(eq(9), eq("6"), eq("9"));
        assertThat(actual).isEqualTo(Collections.emptyList());
    }
}
