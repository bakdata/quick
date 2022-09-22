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

import static com.bakdata.quick.common.TestTypeUtils.newLongData;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

class QueryListArgumentFetcherTest extends FetcherTest {
    @Test
    void shouldFetchListWhenListArgumentOfTypeString() throws JsonProcessingException {
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

        final List<Purchase> itemList = List.of(purchase1, purchase2);
        final String valueList = this.mapper.writeValueAsString(new MirrorValue<>(itemList));

        this.server.enqueue(new MockResponse().setBody(valueList));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Purchase.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);

        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());

        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final ListArgumentFetcher<?, ?> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, true, true);

        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = listArgumentFetcher.get(env);
        assertThat(actual).isEqualTo(itemList);
    }

    @Test
    void shouldFetchListWhenListArgumentOfTypeInt() throws JsonProcessingException {
        final Product product1 = Product.builder()
            .productId(1)
            .name("productTest1")
            .build();

        final Product product2 = Product.builder()
            .productId(2)
            .name("productTest2")
            .build();

        final List<Product> listOfValues = List.of(product1, product2);
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(listOfValues));
        this.server.enqueue(new MockResponse().setBody(body));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Product.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newLongData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final ListArgumentFetcher<?, ?> listArgumentFetcher =
            new ListArgumentFetcher<>("productId", fetcherClient, true, true);

        final Map<String, Object> arguments = Map.of("productId", List.of(1L, 2L));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = listArgumentFetcher.get(env);
        assertThat(actual).isEqualTo(listOfValues);
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(Collections.emptyList()));
        this.server.enqueue(new MockResponse().setBody(body));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Object.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final ListArgumentFetcher<?, ?> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, false, true);

        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final TypeReference<List<Map<String, Object>>> listTypeReference = new TypeReference<>() {};
        final List<?> actual = listArgumentFetcher.get(env);
        final List<?> expected = this.mapper.convertValue(Collections.emptyList(), listTypeReference);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNotNullAndDoesNotHaveNullableElements() throws JsonProcessingException {
        final Purchase purchase1 = Purchase.builder()
            .purchaseId("testId1")
            .productId(1)
            .amount(3)
            .build();

        final List<Object> itemList = new ArrayList<>();
        itemList.add(purchase1);
        itemList.add(null);

        final String body = this.mapper.writeValueAsString(new MirrorValue<>(itemList));
        this.server.enqueue(new MockResponse().setBody(body));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Purchase.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final ListArgumentFetcher<?, ?> listArgumentFetcher =
            new ListArgumentFetcher<>("purchaseId", fetcherClient, true, false);

        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = listArgumentFetcher.get(env);
        final List<?> expected = List.of(purchase1);

        assertThat(actual).isEqualTo(expected);
    }
}
