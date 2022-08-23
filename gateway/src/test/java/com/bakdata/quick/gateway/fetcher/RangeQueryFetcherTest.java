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
import static org.mockito.ArgumentMatchers.anyString;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RangeQueryFetcherTest {
    public static final boolean isNullable = true;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
    private final String host = String.format("localhost:%s", this.server.getPort());

    @Test
    void shouldFetchListWhenListArgumentOfTypeString() throws JsonProcessingException {
        final UserRequest userRequest = UserRequest.builder()
            .userId(1)
            .timestamp(1)
            .requests(3)
            .build();

        final UserRequest userRequest2 = UserRequest.builder()
            .userId(1)
            .timestamp(2)
            .requests(5)
            .build();

        this.server.enqueue(
            new MockResponse().setBody(
                this.mapper.writeValueAsString(new MirrorValue<>(List.of(userRequest, userRequest2)))));

        final DataFetcherClient<UserRequest> fetcherClient = this.createClient(UserRequest.class);

        final RangeQueryFetcher<?> rangeQueryFetcher =
            new RangeQueryFetcher<>("userId", fetcherClient, "timestampFrom","timestampTo", isNullable);

        final Map<String, Object> arguments = Map.of("userId", "1", "timestampFrom", "1", "timestampTo", "2");

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = rangeQueryFetcher.get(env);
        assertThat(actual).isEqualTo(List.of(userRequest, userRequest2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() {
        final MirrorDataFetcherClient fetcherClient = Mockito.mock(MirrorDataFetcherClient.class);

        Mockito.when(fetcherClient.fetchRange(anyString(), anyString(), anyString())).thenReturn(null);

        final RangeQueryFetcher<?> rangeQueryFetcher =
            new RangeQueryFetcher<>("userId", fetcherClient, "timestampFrom","timestampTo", false);

        final Map<String, Object> arguments = Map.of("userId", "1", "timestampFrom", "1", "timestampTo", "2");

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = rangeQueryFetcher.get(env);

        assertThat(actual).isEqualTo(Collections.emptyList());
    }

    @NotNull
    private <T> MirrorDataFetcherClient<T> createClient(final Class<T> clazz) {
        final TypeResolver<T> resolver = new KnownTypeResolver<>(clazz, this.mapper);
        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, resolver);
    }

    @Value
    @Builder
    private static class UserRequest {
        int userId;
        int timestamp;
        int requests;
    }
}
