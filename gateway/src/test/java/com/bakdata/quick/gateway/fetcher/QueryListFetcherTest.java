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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.TestModels.Purchase;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryListFetcherTest {
    @Test
    void shouldFetchListOfStrings() {
        final List<String> values = List.of("abc", "def");

        final PartitionedMirrorClient<?, String> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchAll()).thenReturn(values);
        final DataFetcherClient<?, String> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryListFetcher<?, String> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<String> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(values);
    }

    @Test
    void shouldFetchListOfInteger() {
        final List<Integer> values = List.of(1, 2);

        final PartitionedMirrorClient<?, Integer> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchAll()).thenReturn(values);
        final DataFetcherClient<?, Integer> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryListFetcher<?, Integer> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<Integer> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(values);
    }

    @Test
    void shouldFetchListOfDoubles() {
        final List<Double> values = List.of(0.5, 0.1);

        final PartitionedMirrorClient<?, Double> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchAll()).thenReturn(values);
        final DataFetcherClient<?, Double> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryListFetcher<?, Double> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();
        final List<Double> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(values);
    }

    @Test
    void shouldFetchListOfObjectsWithKeyString() {
        final Purchase purchase1 = Purchase.builder()
            .purchaseId("testId")
            .productId(1)
            .amount(3)
            .build();

        final Purchase purchase2 = Purchase.builder()
            .purchaseId("testId2")
            .productId(2)
            .amount(5)
            .build();

        final List<Purchase> purchaseList = new java.util.ArrayList<>(List.of(purchase1, purchase2));

        final PartitionedMirrorClient<?, Purchase> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchAll()).thenReturn(purchaseList);
        final DataFetcherClient<?, Purchase> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final QueryListFetcher<?, Purchase> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final Map<String, String> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = queryFetcher.get(env);
        assertThat(actual).isEqualTo(purchaseList);
    }
}
