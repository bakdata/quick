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

import static com.bakdata.quick.common.TestTypeUtils.newStringData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

class KeyFieldFetcherTest extends FetcherTest {
    private static TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {};

    @Test
    void shouldFetchModificationValue() throws JsonProcessingException {
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

        final String productJson = this.mapper.writeValueAsString(new MirrorValue<>(product));
        this.server.enqueue(new MockResponse().setBody(productJson));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Product.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);

        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());

        final DataFetcherClient<String, Product> fetcherClient = this.createClient();
        final KeyFieldFetcher<?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "productId", fetcherClient);

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

        final String currencyJson = this.mapper.writeValueAsString(new MirrorValue<>(currency));
        this.server.enqueue(new MockResponse().setBody(currencyJson));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Currency.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);

        final DataFetcherClient<String, ?> fetcherClient = this.createClient();
        final KeyFieldFetcher<?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "currencyId", fetcherClient);

        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());

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

        final PurchaseList purchase = PurchaseList.builder()
            .purchaseId("testId")
            .productIds(List.of(productId1, productId2))
            .build();

        final Product product1 = Product.builder()
            .productId(productId1)
            .prices(List.of(3))
            .build();

        final Product product2 = Product.builder()
            .productId(productId2)
            .prices(List.of(3, 4, 5))
            .build();

        final String valueList = this.mapper.writeValueAsString(new MirrorValue<>(List.of(product1, product2)));
        this.server.enqueue(new MockResponse().setBody(valueList));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Product.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);

        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());

        final DataFetcherClient<String, Product> fetcherClient = this.createClient();
        final KeyFieldFetcher<?> queryFetcher = new KeyFieldFetcher<>(this.mapper, "productIds", fetcherClient);

        final String source = this.mapper.writeValueAsString(purchase);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.readValue(source, OBJECT_TYPE_REFERENCE)).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(List.of(product1, product2));
    }
}
