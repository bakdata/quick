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
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.TestModels.Product;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryKeyArgumentFetcherTest {
    @Test
    void shouldFetchStringValueWithKeyString() {
        final String value = "test";

        final PartitionedMirrorClient<String, String> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq("testId"))).thenReturn(value);
        final DataFetcherClient<String, String> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchIntegerValueWithKeyString() {
        final int value = 5;

        final PartitionedMirrorClient<String, Integer> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq("testId"))).thenReturn(value);
        final DataFetcherClient<String, Integer> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchLongValueWithKeyString() {
        final long value = 5L;

        final PartitionedMirrorClient<String, Long> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq("testId"))).thenReturn(value);
        final DataFetcherClient<String, Long> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchDoubleValue() {
        final double value = 0.5;

        final PartitionedMirrorClient<String, Double> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq("testId"))).thenReturn(value);
        final DataFetcherClient<String, Double> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchObjectValueWithKeyString() {
        final Product<String> product = Product.<String>builder()
            .productId("testId")
            .build();

        final PartitionedMirrorClient<String, Product<String>> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq("testId"))).thenReturn(product);
        final DataFetcherClient<String, Product<String>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(product);
    }
}
