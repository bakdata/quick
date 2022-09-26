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

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.resolver.DoubleResolver;
import com.bakdata.quick.common.resolver.IntegerResolver;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.LongResolver;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

class QueryKeyArgumentFetcherTest extends FetcherTest {

    @Test
    void shouldFetchObjectValue() throws JsonProcessingException {
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .productId(2)
            .amount(3)
            .build();
        final String purchaseJson = this.mapper.writeValueAsString(new MirrorValue<>(purchase));
        this.server.enqueue(new MockResponse().setBody(purchaseJson));

        final TypeResolver<Purchase> knownTypeResolver = new KnownTypeResolver<>(Purchase.class, this.mapper);
        final DataFetcherClient<?, ?> fetcherClient = this.createClient(newStringData(), knownTypeResolver);
        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(purchase);
    }

    @Test
    void shouldFetchStringValue() throws JsonProcessingException {
        final String value = "test";
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<?, ?> fetcherClient = this.createClient(newStringData(), new StringResolver());
        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo("test");
    }

    @Test
    void shouldFetchIntegerValue() throws JsonProcessingException {
        final int value = 5;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));

        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<?, ?> fetcherClient = this.createClient(newStringData(), new IntegerResolver());
        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchLongValue() throws JsonProcessingException {
        final long value = 5L;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<?, ?> fetcherClient = this.createClient(newStringData(), new LongResolver());
        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }

    @Test
    void shouldFetchDoubleValue() throws JsonProcessingException {
        final double value = 0.5;
        final String valueJson = this.mapper.writeValueAsString(new MirrorValue<>(value));
        this.server.enqueue(new MockResponse().setBody(valueJson));

        final DataFetcherClient<?, ?> fetcherClient = this.createClient(newStringData(), new DoubleResolver());
        final QueryKeyArgumentFetcher<?, ?> queryFetcher = new QueryKeyArgumentFetcher<>("purchaseId", fetcherClient,
            true);

        final Map<String, Object> arguments = Map.of("purchaseId", "testId");
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(value);
    }
}
