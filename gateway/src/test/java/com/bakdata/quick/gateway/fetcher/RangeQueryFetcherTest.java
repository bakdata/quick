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
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

class RangeQueryFetcherTest extends FetcherTest {
    @Test
    void shouldFetchRange() throws JsonProcessingException {
        final Product product1 = Product.builder()
            .productId(1)
            .name("productTest1")
            .ratings(4)
            .build();

        final Product product2 = Product.builder()
            .productId(2)
            .name("productTest2")
            .ratings(3)
            .build();

        final List<Product> userRequests = List.of(
            product1,
            product2
        );

        final String userRequestJson = this.mapper.writeValueAsString(new MirrorValue<>(userRequests));
        this.server.enqueue(new MockResponse().setBody(userRequestJson));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Product.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final RangeQueryFetcher<?, ?> rangeQueryFetcher =
            new RangeQueryFetcher<>("userId", fetcherClient, "ratingFrom", "ratingTo", true);

        final Map<String, Object> arguments = Map.of("userId", "1", "ratingFrom", "1", "ratingTo", "4");

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = rangeQueryFetcher.get(env);
        assertThat(actual).isEqualTo(userRequests);
    }

    @Test
    void shouldFetchEmptyListWhenResultIsNullAndReturnTypeIsNotNullable() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(Collections.emptyList()));
        this.server.enqueue(new MockResponse().setBody(body));

        final TypeResolver<?> knownTypeResolver = new KnownTypeResolver<>(Purchase.class, this.mapper);
        final QuickTopicData<?, ?> topicInfo = newQuickTopicData(newStringData(), knownTypeResolver);
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(anyString());
        final DataFetcherClient<String, ?> fetcherClient = this.createClient();

        final RangeQueryFetcher<?, ?> rangeQueryFetcher =
            new RangeQueryFetcher<>("userId", fetcherClient, "timestampFrom", "timestampTo", false);

        final Map<String, Object> arguments = Map.of("userId", "1", "timestampFrom", "1", "timestampTo", "2");

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .localContext(arguments).build();

        final List<?> actual = rangeQueryFetcher.get(env);

        assertThat(actual).isEqualTo(Collections.emptyList());
    }
}
