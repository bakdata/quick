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
//import static org.mockito.ArgumentMatchers.anyString;
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
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import lombok.Builder;
//import lombok.Value;
//import okhttp3.OkHttpClient;
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//class RangeQueryFetcherTest {
//    public static final boolean isNullable = true;
//    private final ObjectMapper mapper = new ObjectMapper();
//    private final MockWebServer server = new MockWebServer();
//    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
//    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
//    private final String host = String.format("localhost:%s", this.server.getPort());
//
//    @BeforeEach
//    void initRouterAndMirror() throws JsonProcessingException {
//        // mapping from partition to host for initializing PartitionRouter
//        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, this.host, 2, this.host));
//        this.server.enqueue(new MockResponse().setBody(routerBody));
//    }
//
//    @Test
//    void shouldFetchRange() throws JsonProcessingException {
//        final UserRequest userRequest1 = UserRequest.builder()
//            .userId(1)
//            .timestamp(1)
//            .requests(Request.builder().count(10).successful(8).build())
//            .build();
//        final UserRequest userRequest2 = UserRequest.builder()
//            .userId(1)
//            .timestamp(2)
//            .requests(Request.builder().count(10).successful(8).build())
//            .build();
//
//        final List<UserRequest> userRequests = List.of(
//            userRequest1,
//            userRequest2
//        );
//
//        final String userRequestJson = this.mapper.writeValueAsString(new MirrorValue<>(userRequests));
//        this.server.enqueue(new MockResponse().setBody(userRequestJson));
//
//        final DataFetcherClient<UserRequest> fetcherClient = this.createClient();
//
//        final RangeQueryFetcher<?> rangeQueryFetcher =
//            new RangeQueryFetcher<>("userId", fetcherClient, "timestampFrom", "timestampTo", isNullable);
//
//        final Map<String, Object> arguments = Map.of("userId", "1", "timestampFrom", "1", "timestampTo", "2");
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = rangeQueryFetcher.get(env);
//        assertThat(actual).isEqualTo(userRequests);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() {
//        final MirrorDataFetcherClient fetcherClient = Mockito.mock(MirrorDataFetcherClient.class);
//
//        Mockito.when(fetcherClient.fetchRange(anyString(), anyString(), anyString())).thenReturn(null);
//
//        final RangeQueryFetcher<?> rangeQueryFetcher =
//            new RangeQueryFetcher<>("userId", fetcherClient, "timestampFrom", "timestampTo", false);
//
//        final Map<String, Object> arguments = Map.of("userId", "1", "timestampFrom", "1", "timestampTo", "2");
//
//        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
//            .localContext(arguments).build();
//
//        final List<?> actual = rangeQueryFetcher.get(env);
//
//        assertThat(actual).isEqualTo(Collections.emptyList());
//    }
//
//    private MirrorDataFetcherClient<UserRequest> createClient() {
//        final TypeResolver<UserRequest> resolver = new KnownTypeResolver<>(UserRequest.class, this.mapper);
//        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, resolver);
//    }
//
//    @Value
//    @Builder
//    private static class UserRequest {
//        int userId;
//        int timestamp;
//        Request requests;
//    }
//
//    @Value
//    @Builder
//    private static class Request {
//        int count;
//        int successful;
//    }
//}
