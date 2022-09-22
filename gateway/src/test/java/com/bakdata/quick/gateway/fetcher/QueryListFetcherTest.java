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


import static com.bakdata.quick.common.TestTypeUtils.newDoubleData;
import static com.bakdata.quick.common.TestTypeUtils.newIntegerData;
import static com.bakdata.quick.common.TestTypeUtils.newLongData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

class QueryListFetcherTest extends FetcherTest {

    @Test
    void shouldFetchListOfObjects() throws JsonProcessingException {
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
        final String purchaseJson = this.mapper.writeValueAsString(new MirrorValue<>(purchaseList));
        this.server.enqueue(new MockResponse().setBody(purchaseJson));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Purchase.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newLongData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final QueryListFetcher<?, ?> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final Map<String, String> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = queryFetcher.get(env);
        assertThat(actual).isEqualTo(purchaseList);
    }

    @Test
    void shouldFetchListOfStrings() throws JsonProcessingException {
        final List<String> list = List.of("abc", "def");
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));
        this.server.enqueue(new MockResponse().setBody(listJson));

        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newLongData(), new StringResolver());
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final QueryListFetcher<?, ?> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<?> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(List.of("abc", "def"));
    }

    @Test
    void shouldFetchListOfInteger() throws JsonProcessingException {
        final List<Integer> list = List.of(1, 2);
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));

        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newIntegerData(), new IntegerResolver());
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final QueryListFetcher<?, ?> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<?> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(List.of(1, 2));
    }

    @Test
    void shouldFetchListOfDoubles() throws JsonProcessingException {
        final List<Double> list = List.of(0.5, 0.1);
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));

        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newDoubleData(), new DoubleResolver());
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final QueryListFetcher<?, ?> queryFetcher = new QueryListFetcher<>(fetcherClient, true, true);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();
        final List<?> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(List.of(0.5, 0.1));
    }
}
