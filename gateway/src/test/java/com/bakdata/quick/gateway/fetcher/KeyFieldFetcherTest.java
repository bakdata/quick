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

import com.bakdata.quick.avro.PurchaseStatsAvro;
import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.TestModels.Product;
import com.bakdata.quick.gateway.fetcher.TestModels.Purchase;
import com.bakdata.quick.testutil.PurchaseStatsProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.language.TypeName;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeyFieldFetcherTest {
    private final ObjectMapper mapper = new ObjectMapper();


    @ParameterizedTest
    @MethodSource("provideValues")
    <T> void shouldFetchModificationValueWhenReturnTypeIsJson(final T productId, final TypeName typeName) {
        final Purchase<T> purchase = Purchase.<T>builder()
            .purchaseId("testId")
            .productId(productId)
            .amount(3)
            .build();

        final Product<T> product = Product.<T>builder()
            .productId(productId)
            .build();

        final PartitionedMirrorClient<T, Product<T>> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq(productId))).thenReturn(product);
        final DataFetcherClient<T, Product<T>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher =
            new KeyFieldFetcher<>(this.mapper, "productId", fetcherClient, typeName);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.convertValue(purchase, Map.class))
            .build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(product);
    }

    @Test
    void shouldFetchNestModificationValueWhenReturnTypeIsAvro() {
        final int productId = 5;
        final PurchaseStatsAvro purchase = PurchaseStatsAvro.newBuilder()
            .setId("purchase1")
            .setAmount(1)
            .setProductId(productId)
            .build();

        final Product<Integer> product = Product.<Integer>builder()
            .productId(productId)
            .build();

        final PartitionedMirrorClient<Integer, Product<Integer>> partitionedMirrorClient =
            mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq(productId))).thenReturn(product);
        final DataFetcherClient<Integer, Product<Integer>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher =
            new KeyFieldFetcher<>(this.mapper, "productId", fetcherClient,
                new TypeName(Scalars.GraphQLInt.getName()));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(purchase).build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(product);
    }
    @Test
    void shouldFetchNestModificationValueWhenReturnTypeIsProto() {
        final int productId = 5;
        final PurchaseStatsProto purchase = PurchaseStatsProto.newBuilder()
            .setId("purchase1")
            .setAmount(1)
            .setProductId(productId)
            .build();

        final Product<Integer> product = Product.<Integer>builder()
            .productId(productId)
            .build();

        final PartitionedMirrorClient<Integer, Product<Integer>> partitionedMirrorClient =
            mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq(productId))).thenReturn(product);
        final DataFetcherClient<Integer, Product<Integer>> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher =
            new KeyFieldFetcher<>(this.mapper, "productId", fetcherClient,
                new TypeName(Scalars.GraphQLInt.getName()));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(purchase).build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(product);
    }

    private static Stream<Arguments> provideValues() {
        return Stream.of(
            Arguments.of(1, new TypeName(Scalars.GraphQLInt.getName())),
            Arguments.of("abc", new TypeName(Scalars.GraphQLString.getName())),
            Arguments.of(1L, new TypeName(ExtendedScalars.GraphQLLong.getName())),
            Arguments.of(15.8, new TypeName(Scalars.GraphQLFloat.getName()))
        );
    }
}
