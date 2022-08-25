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

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryListFetcherTest {

    public static final boolean isNullable = true;
    public static final boolean hasNullableElements = true;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
    private final String host = String.format("localhost:%s", this.server.getPort());

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // mapping from partition to host for initializing PartitionRouter
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, this.host, 2, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
    }

    @Test
    void shouldFetchListOfObjects() throws Exception {
        final Purchase purchase1 = Purchase.builder()
            .purchaseId("testId")
            .productId("productTestId")
            .amount(3)
            .build();

        final Purchase purchase2 = Purchase.builder()
            .purchaseId("testId2")
            .productId("productTestId2")
            .amount(5)
            .build();

        final List<Purchase> purchaseList = new java.util.ArrayList<>(List.of(purchase1, purchase2));
        final String purchaseJson = this.mapper.writeValueAsString(new MirrorValue<>(purchaseList));
        this.server.enqueue(new MockResponse().setBody(purchaseJson));
        this.server.enqueue(new MockResponse().setBody(purchaseJson));

        final DataFetcherClient<?> fetcherClient =
            this.createClient(new KnownTypeResolver<>(Purchase.class, this.mapper));
        final QueryListFetcher<?> queryFetcher =
            new QueryListFetcher<>(fetcherClient, isNullable, hasNullableElements);
        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<Purchase> doublePurchaseList = Streams.concat(
            purchaseList.stream(), purchaseList.stream()).collect(Collectors.toList());
        final List<?> actual = queryFetcher.get(env);
        assertThat(actual).isEqualTo(doublePurchaseList);
    }


    @Test
    void shouldFetchListOfStrings() throws Exception {

        final List<String> list = List.of("abc", "def");
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));
        this.server.enqueue(new MockResponse().setBody(listJson));

        final DataFetcherClient<?> fetcherClient = this.createClient(new StringResolver());
        final QueryListFetcher<?> queryFetcher = new QueryListFetcher<>(fetcherClient, isNullable, hasNullableElements);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<?> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(List.of("abc", "def", "abc", "def"));
    }

    @Test
    void shouldFetchListOfInteger() throws Exception {
        final List<Integer> list = List.of(1, 2);
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));
        this.server.enqueue(new MockResponse().setBody(listJson));

        final DataFetcherClient<?> fetcherClient = this.createClient(new IntegerResolver());
        final QueryListFetcher<?> queryFetcher = new QueryListFetcher<>(fetcherClient, isNullable, hasNullableElements);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();

        final List<?> fetcherResult = queryFetcher.get(env);

        assertThat(fetcherResult).isEqualTo(List.of(1, 2, 1, 2));
    }

    @Test
    void shouldFetchListOfDoubles() throws Exception {
        final List<Double> list = List.of(0.5, 0.1);
        final String listJson = this.mapper.writeValueAsString(new MirrorValue<>(list));
        this.server.enqueue(new MockResponse().setBody(listJson));
        // we have the mirror and the replica so the mocked response must be retrieved twice
        this.server.enqueue(new MockResponse().setBody(listJson));
        final DataFetcherClient<?> fetcherClient = this.createClient(new DoubleResolver());
        final QueryListFetcher<?> queryFetcher = new QueryListFetcher<>(fetcherClient, isNullable,
            hasNullableElements);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build();
        final List<?> fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(List.of(0.5, 0.1, 0.5, 0.1));
    }

    private <T> MirrorDataFetcherClient<T> createClient(final TypeResolver<T> type) {
        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, type);
    }

    @Data
    @Builder
    static class Purchase {
        private String purchaseId;
        private String productId;
        private int amount;
    }
}
