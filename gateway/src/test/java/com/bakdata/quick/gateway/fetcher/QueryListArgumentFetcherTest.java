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
import com.bakdata.quick.gateway.fetcher.TestModels.Purchase;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QueryListArgumentFetcherTest {

    @ParameterizedTest
    @MethodSource("provideValues")
    <T> void shouldFetchListWhenListArgumentOfTypeIntWithKeyLong(final List<T> productIds) {
        final Product<T> product1 = Product.<T>builder()
            .productId(productIds.get(0))
            .name("productTest1")
            .build();

        final Product<T> product2 = Product.<T>builder()
            .productId(productIds.get(1))
            .name("productTest2")
            .build();

        final List<Product<T>> productList = List.of(product1, product2);

        final PartitionedMirrorClient<T, Product<T>> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(productIds))).thenReturn(productList);
        final DataFetcherClient<T, Product<T>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<T, Product<T>> listArgumentFetcher =
            new ListArgumentFetcher<>("productId", fetcherClient, true, true);

        final Map<String, Object> arguments = Map.of("productId", productIds);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Product<T>> actual = listArgumentFetcher.get(env);
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
        final Product<String> product = Product.<String>builder()
            .productId("testId1")
            .name("productTest1")
            .build();

        final List<String> idList = List.of("testId1", "testId2");
        final List<Product<String>> itemList = new ArrayList<>();
        itemList.add(product);
        itemList.add(null);

        final PartitionedMirrorClient<String, Product<String>> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(idList))).thenReturn(itemList);
        final DataFetcherClient<String, Product<String>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListArgumentFetcher<String, Product<String>> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, true, false);

        final Map<String, Object> arguments = Map.of("purchaseId", idList);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Product<String>> actual = listArgumentFetcher.get(env);
        final List<Product<String>> expected = List.of(product);

        assertThat(actual).isEqualTo(expected);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
            Arguments.of(List.of(1, 2)),
            Arguments.of(List.of("abc", "efg")),
            Arguments.of(List.of(1L, 2L))
        );
    }
}
