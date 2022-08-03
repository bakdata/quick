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
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class QueryKeyArgumentFetcherTest {

    public static final boolean isNullable = true;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
    private final String host = String.format("localhost:%s", this.server.getPort());

    @Test
    void shouldFetchObjectValue() throws IOException {
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .productId("productTestId")
            .amount(3)
            .build();
        final String purchaseJson = this.mapper.writeValueAsString(new MirrorValue<>(purchase));
        this.server.enqueue(new MockResponse().setBody(purchaseJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final QueryKeyArgumentFetcher queryFetcher = new QueryKeyArgumentFetcher("purchaseId", fetcherClient,
            isNullable);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final JsonNode fetcherResult = queryFetcher.get(env);
        assertThat(Objects.requireNonNull(fetcherResult).get("purchaseId").asText()).isEqualTo(purchase.purchaseId);
        assertThat(fetcherResult.get("productId").asText()).isEqualTo(purchase.productId);
        assertThat(fetcherResult.get("amount").asInt()).isEqualTo(purchase.amount);
    }

    // TODO: How to handle string values?
    @Test
    void shouldFetchStringValue() throws JsonProcessingException {
        final String value = "test";
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final QueryKeyArgumentFetcher queryFetcher = new QueryKeyArgumentFetcher("purchaseId", fetcherClient,
            isNullable);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final JsonNode fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult.get("testId").asText()).isEqualTo("test");
    }

    @Test
    void shouldFetchIntegerValue() throws Exception {
        final int value = 5;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));

        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final QueryKeyArgumentFetcher queryFetcher = new QueryKeyArgumentFetcher("purchaseId", fetcherClient,
            isNullable);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final JsonNode fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult.asInt()).isEqualTo(value);
    }


    @Test
    void shouldFetchLongValue() throws Exception {
        final long value = 5L;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final QueryKeyArgumentFetcher queryFetcher = new QueryKeyArgumentFetcher("purchaseId", fetcherClient,
            isNullable);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }


    @Test
    void shouldFetchDoubleValue() throws Exception {
        final double value = 0.5;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final QueryKeyArgumentFetcher queryFetcher = new QueryKeyArgumentFetcher("purchaseId", fetcherClient,
            isNullable);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    private MirrorDataFetcherClient createClient() {
        final TypeResolver<JsonNode> type = new KnownTypeResolver<>(JsonNode.class, mapper);
        return new MirrorDataFetcherClient(this.host, this.client, this.mirrorConfig, type);
    }


    @Data
    @Builder
    private static class Purchase {
        private String purchaseId;
        private String productId;
        private int amount;
    }
}
