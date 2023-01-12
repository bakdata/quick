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

import com.bakdata.quick.avro.PurchaseListAvro;
import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.TestModels.Product;
import com.bakdata.quick.gateway.fetcher.TestModels.PurchaseList;
import com.bakdata.quick.testutil.PurchaseListProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ListFieldFetcherTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReturnCorrectListWhenReturnTypeIsAvroWithKeysOfInt() {
        final int productId1 = 1;
        final int productId2 = 3;

        final List<Integer> productIds = List.of(productId1, productId2);
        final PurchaseListAvro purchase = PurchaseListAvro.newBuilder()
            .setId("testId")
            .setProductIds(productIds)
            .build();

        final Product<Integer> product1 = Product.<Integer>builder()
            .productId(productId1)
            .build();

        final Product<Integer> product2 = Product.<Integer>builder()
            .productId(productId2)
            .build();

        final List<Product<Integer>> products = List.of(product1, product2);

        final PartitionedMirrorClient<Integer, Product<Integer>> partitionedMirrorClient =
            mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(productIds))).thenReturn(products);
        final DataFetcherClient<Integer, Product<Integer>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListFieldFetcher<Integer, Product<Integer>> queryFetcher =
            new ListFieldFetcher<>("productIds", fetcherClient);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(purchase).build();
        final List<Product<Integer>> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(products);
    }

    @Test
    void shouldReturnCorrectListWhenReturnTypeIsProtobufWithKeysOfInt() {
        final int productId1 = 1;
        final int productId2 = 3;

        final List<Integer> productIds = List.of(productId1, productId2);
        final PurchaseListProto purchase = PurchaseListProto.newBuilder()
            .setId("testId")
            .addAllProductIds(productIds)
            .build();

        final Product<Integer> product1 = Product.<Integer>builder()
            .productId(productId1)
            .build();

        final Product<Integer> product2 = Product.<Integer>builder()
            .productId(productId2)
            .build();

        final List<Product<Integer>> products = List.of(product1, product2);

        final PartitionedMirrorClient<Integer, Product<Integer>> partitionedMirrorClient =
            mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(productIds))).thenReturn(products);
        final DataFetcherClient<Integer, Product<Integer>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListFieldFetcher<Integer, Product<Integer>> queryFetcher =
            new ListFieldFetcher<>("productIds", fetcherClient);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(purchase).build();
        final List<Product<Integer>> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(products);
    }

    @ParameterizedTest
    @MethodSource("provideValues")
    <T> void shouldReturnCorrectListWhenReturnTypeIsJson(final List<T> productIds) {
        final PurchaseList<T> purchase = PurchaseList.<T>builder()
            .purchaseId("testId")
            .productIds(productIds)
            .build();

        final Product<T> product1 = Product.<T>builder()
            .productId(productIds.get(0))
            .build();

        final Product<T> product2 = Product.<T>builder()
            .productId(productIds.get(1))
            .build();

        final List<Product<T>> products = List.of(product1, product2);

        final PartitionedMirrorClient<T, Product<T>> partitionedMirrorClient =
            mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(productIds))).thenReturn(products);
        final DataFetcherClient<T, Product<T>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final ListFieldFetcher<T, Product<T>> queryFetcher =
            new ListFieldFetcher<>("productIds", fetcherClient);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.convertValue(purchase, Map.class)).build();
        final List<Product<T>> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(products);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
            Arguments.of(List.of(1, 2)),
            Arguments.of(List.of("abc", "efg")),
            Arguments.of(List.of(1L, 2L))
        );
    }
}
