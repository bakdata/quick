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
import lombok.Data;
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


    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // mapping from partition to host for initializing PartitionRouter
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(0, this.host, 1, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
    }

    protected  <K, V> MirrorDataFetcherClient<K, V> createClient() {
        return new MirrorDataFetcherClient<>(this.host, this.client, this.mirrorConfig, this.typeService);
    }

    protected static QuickTopicData<?, ?> newQuickTopicData(final TypeResolver<?> typeResolver) {
        final QuickData<?> valueData = new QuickData<>(QuickTopicType.AVRO, null, typeResolver);
        return new QuickTopicData<>("topic", TopicWriteType.MUTABLE, newStringData(), valueData);
    }


    @Data
    @Builder
    protected static class PurchaseList {
        private String purchaseId;
        private List<Integer> productIds;
    }

    @Data
    @Builder
    protected static class Purchase {
        private String purchaseId;
        private int productId;
        private int amount;
        private Price price;
    }

    @Data
    @Builder
    protected static class Product {
        private int productId;
        private List<Integer> prices;
    }

    @Data
    @Builder
    protected static class Price {
        private String currencyId;
        private double value;
    }

    @Data
    @Builder
    protected static class Currency {
        private String currencyId;
        private String currency;
        private double rate;
    }
}
