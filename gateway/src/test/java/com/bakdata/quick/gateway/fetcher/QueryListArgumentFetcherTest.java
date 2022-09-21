///*
// *    Copyright 2022 bakdata GmbH
// *
// *    Licensed under the Apache License, Version 2.0 (the "License");
// *    you may not use this file except in compliance with the License.
// *    You may obtain a copy of the License at
// *
// *        http://www.apache.org/licenses/LICENSE-2.0
// *
// *    Unless required by applicable law or agreed to in writing, software
// *    distributed under the License is distributed on an "AS IS" BASIS,
// *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *    See the License for the specific language governing permissions and
// *    limitations under the License.
// */
//
//package com.bakdata.quick.gateway.fetcher;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//
//import com.bakdata.quick.common.api.client.HttpClient;
//import com.bakdata.quick.common.api.model.mirror.MirrorValue;
//import com.bakdata.quick.common.config.MirrorConfig;
//import com.bakdata.quick.common.resolver.KnownTypeResolver;
//import com.bakdata.quick.common.resolver.TypeResolver;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import graphql.schema.DataFetchingEnvironment;
//import graphql.schema.DataFetchingEnvironmentImpl;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import lombok.Builder;
//import lombok.Value;
//import okhttp3.OkHttpClient;
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.jetbrains.annotations.NotNull;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//class QueryListArgumentFetcherTest {
//
//    public static final boolean isNullable = true;
//    public static final boolean hasNullableElements = true;
//    private final ObjectMapper mapper = new ObjectMapper();
//    private final MockWebServer server = new MockWebServer();
//    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
//    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
//    private final String host = String.format("localhost:%s", this.server.getPort());
//
//    @BeforeEach
//    void initRouterAndMirror() throws JsonProcessingException {
//        // mapping from partition to host for initializing PartitionRouter
//        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(0, this.host,1, this.host));
//        this.server.enqueue(new MockResponse().setBody(routerBody));
//    }
//
//    @Test
//    void shouldFetchListWhenListArgumentOfTypeString() throws JsonProcessingException {
//        final Purchase purchase1 = Purchase.builder()
//            .purchaseId("testId1")
//            .productId(1)
//            .amount(3)
//            .build();
//
//        final Purchase purchase2 = Purchase.builder()
//            .purchaseId("testId2")
//            .productId(2)
//            .amount(3)
//            .build();
//
//        this.server.enqueue(new MockResponse().setBody(this.mapper.writeValueAsString(new MirrorValue<>(purchase1))));
//        this.server.enqueue(new MockResponse().setBody(this.mapper.writeValueAsString(new MirrorValue<>(purchase2))));
//
//        final DataFetcherClient<Purchase> fetcherClient = this.createClient(Purchase.class);
//
//        final ListArgumentFetcher<?> listArgumentFetcher =
//            new ListArgumentFetcher<>("purchaseId", fetcherClient, isNullable, hasNullableElements);
//
//        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = listArgumentFetcher.get(env);
//        assertThat(actual).isEqualTo(List.of(purchase1, purchase2));
//    }
//
//    // This test is temporarily disabled because PartitionedMirrorClient can only work with a single type.
//    // The reason is the findHost function in the underlying PartitionRouter that needs the serializer
//    // of a specific type.
//    @Disabled
//    @Test
//    void shouldFetchListWhenListArgumentOfTypeInt() throws JsonProcessingException {
//        final Product product1 = Product.builder()
//            .productId(1)
//            .name("productTest1")
//            .build();
//
//        final Product product2 = Product.builder()
//            .productId(2)
//            .name("productTest2")
//            .build();
//
//        this.server.enqueue(
//            new MockResponse().setBody(this.mapper.writeValueAsString(new MirrorValue<>(List.of(product1, product2)))));
//
//        final DataFetcherClient<Product> fetcherClient = this.createClient(Product.class);
//
//        final ListArgumentFetcher<?> listArgumentFetcher =
//            new ListArgumentFetcher<>("productId", fetcherClient, isNullable, hasNullableElements);
//
//        final Map<String, Object> arguments = Map.of("productId", List.of(1L, 2L));
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = listArgumentFetcher.get(env);
//        assertThat(actual).isEqualTo(List.of(product1, product2));
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() {
//        final MirrorDataFetcherClient fetcherClient = Mockito.mock(MirrorDataFetcherClient.class);
//
//        Mockito.when(fetcherClient.fetchResults(any())).thenReturn(null);
//
//        final ListArgumentFetcher<?> listArgumentFetcher =
//            new ListArgumentFetcher<>("purchaseId", fetcherClient, false, hasNullableElements);
//
//        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = listArgumentFetcher.get(env);
//        final List<?> expected =
//            this.mapper.convertValue(Collections.emptyList(), DataFetcherClient.LIST_TYPE_REFERENCE);
//
//        assertThat(actual).isEqualTo(expected);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    void shouldFetchEmptyListWhenResultIsNotNullAndDoesNotHaveNullableElements() {
//        final Purchase purchase1 = Purchase.builder()
//            .purchaseId("testId1")
//            .productId(1)
//            .amount(3)
//            .build();
//
//        final MirrorDataFetcherClient fetcherClient = Mockito.mock(MirrorDataFetcherClient.class);
//
//        final List<Object> itemList = new ArrayList<>();
//        itemList.add(purchase1);
//        itemList.add(null);
//
//        Mockito.when(fetcherClient.fetchResults(any())).thenReturn(itemList);
//
//        final ListArgumentFetcher<?> listArgumentFetcher =
//            new ListArgumentFetcher<>("purchaseId", fetcherClient, isNullable, false);
//
//        final Map<String, Object> arguments = Map.of("purchaseId", List.of("testId1", "testId2"));
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = listArgumentFetcher.get(env);
//        final List<?> expected = List.of(purchase1);
//
//        assertThat(actual).isEqualTo(expected);
//    }
//
//    @NotNull
//    private <T> MirrorDataFetcherClient<T> createClient() {
//        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, resolver);
//    }
//
//    @Value
//    @Builder
//    private static class Purchase {
//        String purchaseId;
//        int productId;
//        int amount;
//    }
//
//    @Value
//    @Builder
//    private static class Product {
//        int productId;
//        String name;
//    }
//}
