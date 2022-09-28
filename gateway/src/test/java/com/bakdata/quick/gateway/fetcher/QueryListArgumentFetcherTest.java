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
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueryListArgumentFetcherTest extends FetcherTest {
    @Test
    void shouldFetchListWhenListArgumentOfTypeStringWithKeyString() {
        final Purchase purchase1 = Purchase.builder()
            .purchaseId("testId1")
            .productId(1)
            .amount(3)
            .build();

        final Purchase purchase2 = Purchase.builder()
            .purchaseId("testId2")
            .productId(2)
            .amount(3)
            .build();

        final List<String> idList = List.of("testId1", "testId2");
        final List<Purchase> purchaseList = List.of(purchase1, purchase2);

        final PartitionedMirrorClient<String, Purchase> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(idList))).thenReturn(purchaseList);
        final DataFetcherClient<String, Purchase> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<String , Purchase> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, true, true);

        final Map<String, Object> arguments = Map.of("purchaseId", idList);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Purchase> actual = listArgumentFetcher.get(env);
        assertThat(actual).isEqualTo(purchaseList);
    }

    @Test
    void shouldFetchListWhenListArgumentOfTypeIntWithKeyLong() {
        final Product product1 = Product.builder()
            .productId(1)
            .name("productTest1")
            .build();

        final Product product2 = Product.builder()
            .productId(2)
            .name("productTest2")
            .build();

        final List<Long> idList = List.of(1L, 2L);
        final List<Product> productList = List.of(product1, product2);

        final PartitionedMirrorClient<Long, Product> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(idList))).thenReturn(productList);
        final DataFetcherClient<Long, Product> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<Long, Product> listArgumentFetcher =
            new ListArgumentFetcher<>("productId", fetcherClient, true, true);

        final Map<String, Object> arguments = Map.of("productId", idList);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Product> actual = listArgumentFetcher.get(env);
        assertThat(actual).isEqualTo(productList);
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() {
        final List<String> idList = List.of("testId1", "testId2");

        final PartitionedMirrorClient<String, String> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(idList))).thenReturn(Collections.emptyList());
        final DataFetcherClient<String, String> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<String, String> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, false, true);

        final Map<String, Object> arguments = Map.of("purchaseId", idList);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final List<String> actual = listArgumentFetcher.get(env);

        assertThat(actual).isEqualTo(Collections.emptyList());
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNotNullAndDoesNotHaveNullableElements() {
        final Purchase purchase1 = Purchase.builder()
            .purchaseId("testId1")
            .productId(1)
            .amount(3)
            .build();

        final List<String> idList = List.of("testId1", "testId2");
        final List<Purchase> itemList = new ArrayList<>();
        itemList.add(purchase1);
        itemList.add(null);

        final PartitionedMirrorClient<String, Purchase> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(idList))).thenReturn(itemList);
        final DataFetcherClient<String, Purchase> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<String, Purchase> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, true, false);

        final Map<String, Object> arguments = Map.of("purchaseId", idList);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Purchase> actual = listArgumentFetcher.get(env);
        final List<Purchase> expected = List.of(purchase1);

        assertThat(actual).isEqualTo(expected);
    }
}
