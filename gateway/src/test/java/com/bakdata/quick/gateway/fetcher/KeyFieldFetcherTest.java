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
import com.bakdata.quick.gateway.fetcher.TestModels.Currency;
import com.bakdata.quick.gateway.fetcher.TestModels.Price;
import com.bakdata.quick.gateway.fetcher.TestModels.Product;
import com.bakdata.quick.gateway.fetcher.TestModels.Purchase;
import com.bakdata.quick.gateway.fetcher.TestModels.PurchaseList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyFieldFetcherTest {
    private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {};
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldFetchModificationValue() {
        final int productId = 5;
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .productId(productId)
            .amount(3)
            .build();

        final Product product = Product.builder()
            .productId(productId)
            .prices(List.of(3))
            .build();

        final PartitionedMirrorClient<Integer, Product> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq(5))).thenReturn(product);
        final DataFetcherClient<Integer, Product> fetcherClient = new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "productId", fetcherClient);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.convertValue(purchase, OBJECT_TYPE_REFERENCE))
            .build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(product);
    }

    @Test
    void shouldFetchNestModificationValue() throws JsonProcessingException {
        final String currencyId = "EUR";
        final Currency currency = Currency.builder()
            .currencyId(currencyId)
            .currency("euro")
            .rate(0.5)
            .build();
        final Price price = Price.builder()
            .currencyId(currencyId)
            .value(20.0)
            .build();
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .price(price)
            .amount(3)
            .build();

        final PartitionedMirrorClient<String, Currency> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValue(eq(currencyId))).thenReturn(currency);
        final DataFetcherClient<String, Currency> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "currencyId", fetcherClient);

        final String source = this.mapper.writeValueAsString(purchase);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.readValue(source, OBJECT_TYPE_REFERENCE)).build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(currency);
    }

    @Test
    void shouldResolveTypeInList() throws JsonProcessingException {
        final int productId1 = 1;
        final int productId2 = 3;

        final List<Integer> productIds = List.of(productId1, productId2);
        final PurchaseList purchase = PurchaseList.builder()
            .purchaseId("testId")
            .productIds(productIds)
            .build();

        final Product product1 = Product.builder()
            .productId(productId1)
            .prices(List.of(3))
            .build();

        final Product product2 = Product.builder()
            .productId(productId2)
            .prices(List.of(3, 4, 5))
            .build();

        final List<Product> products = List.of(product1, product2);

        final PartitionedMirrorClient<Integer, Product> partitionedMirrorClient = mock(PartitionedMirrorClient.class);
        when(partitionedMirrorClient.fetchValues(eq(productIds))).thenReturn(products);
        final DataFetcherClient<Integer, Product> fetcherClient =
            new MirrorDataFetcherClient<>(new Lazy<>(() -> partitionedMirrorClient));

        final KeyFieldFetcher<?, ?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "productIds", fetcherClient);

        final String source = this.mapper.writeValueAsString(purchase);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.readValue(source, OBJECT_TYPE_REFERENCE)).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(products);
    }
}
