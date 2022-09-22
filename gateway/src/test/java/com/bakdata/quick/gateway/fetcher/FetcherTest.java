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

import static org.mockito.Mockito.mock;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;

public abstract class FetcherTest {
    protected final ObjectMapper mapper = new ObjectMapper();

    protected final MockWebServer server = new MockWebServer();
    protected final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    protected final TopicTypeService typeService = mock(TopicTypeService.class);
    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();
    private final String host = String.format("localhost:%s", this.server.getPort());


    // TODO: This tests only one host. Test for two different hosts.
    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // mapping from partition to host for initializing PartitionRouter
        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host, 1, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
    }

    protected <K, V> MirrorDataFetcherClient<K, V> createClient() {
        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, this.typeService);
    }

    protected static QuickTopicData<?, ?> newQuickTopicData(final QuickData<?> keyData,
        final TypeResolver<?> typeResolver) {
        final QuickData<?> valueData = new QuickData<>(QuickTopicType.AVRO, null, typeResolver);
        return new QuickTopicData<>("topic", TopicWriteType.MUTABLE, keyData, valueData);
    }

    @Value
    @Builder
    protected static class PurchaseList {
        String purchaseId;
        List<Integer> productIds;
    }

    @Value
    @Builder
    protected static class Purchase {
        String purchaseId;
        int productId;
        int amount;
        Price price;
    }

    @Value
    @Builder
    protected static class Product {
        int productId;
        String name;
        List<Integer> prices;
        int ratings;
    }

    @Value
    @Builder
    protected static class Price {
        String currencyId;
        double value;
    }

    @Value
    @Builder
    protected static class Currency {
        String currencyId;
        String currency;
        double rate;
    }

//    @Value
//    @Builder
//    protected static class UserRequest {
//        int userId;
//        int timestamp;
//        Request requests;
//    }
//
//    @Value
//    @Builder
//    protected static class Request {
//        int count;
//        int successful;
//    }
}
