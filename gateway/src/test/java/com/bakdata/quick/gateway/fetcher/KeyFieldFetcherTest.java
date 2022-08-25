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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class KeyFieldFetcherTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("localhost:%s", this.server.getPort());
    private final MirrorConfig mirrorConfig = MirrorConfig.directAccess();

    @Test
    void shouldFetchModificationValue() throws Exception {
        final int productId = 5;
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .productId(productId)
            .amount(3)
            .build();

        final Product product = Product.builder()
            .productId(productId)
            .prices(List.of(3))
            .build();

        final String productJson = this.mapper.writeValueAsString(new MirrorValue<>(product));
        final JsonNode productJsonNode = this.mapper.valueToTree(product);
        this.server.enqueue(new MockResponse().setBody(productJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final KeyFieldFetcher queryFetcher = new KeyFieldFetcher(this.mapper, "productId", fetcherClient);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.convertValue(purchase, DataFetcherClient.OBJECT_TYPE_REFERENCE))
            .build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(productJsonNode);
    }


    @Test
    void shouldFetchNestModificationValue() throws JsonProcessingException {
        final String currencyId = "EUR";
        final Currency currency = Currency.builder()
            .currencyId(currencyId)
            .currency("euro")
            .rate(0.5)
            .build();
        final Price price = Price.builder()
            .currencyId(currencyId)
            .value(20.0)
            .build();
        final Purchase purchase = Purchase.builder()
            .purchaseId("testId")
            .price(price)
            .amount(3)
            .build();

        final String currencyJson = this.mapper.writeValueAsString(new MirrorValue<>(currency));
        final JsonNode currencyJsonNode = this.mapper.valueToTree(currency);
        this.server.enqueue(new MockResponse().setBody(currencyJson));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final KeyFieldFetcher queryFetcher = new KeyFieldFetcher(this.mapper, "currencyId", fetcherClient);
        final String source = this.mapper.writeValueAsString(purchase);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.readValue(source, DataFetcherClient.OBJECT_TYPE_REFERENCE)).build();

        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(currencyJsonNode);
    }

    @Test
    void shouldResolveTypeInList() throws JsonProcessingException {
        final int productId1 = 1;
        final int productId2 = 3;

        final PurchaseList purchase = PurchaseList.builder()
            .purchaseId("testId")
            .productIds(List.of(productId1, productId2))
            .build();

        final Product product1 = Product.builder()
            .productId(productId1)
            .prices(List.of(3))
            .build();

        final Product product2 = Product.builder()
            .productId(productId2)
            .prices(List.of(3, 4, 5))
            .build();

        final JsonNode products = this.mapper.valueToTree(List.of(product1, product2));
        this.server.enqueue(
            new MockResponse().setBody(this.mapper.writeValueAsString(new MirrorValue<>(List.of(product1, product2)))));

        final DataFetcherClient<JsonNode> fetcherClient = this.createClient();
        final KeyFieldFetcher queryFetcher =
            new KeyFieldFetcher(this.mapper, "productIds", fetcherClient);
        final String source = this.mapper.writeValueAsString(purchase);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .source(this.mapper.readValue(source, DataFetcherClient.OBJECT_TYPE_REFERENCE)).build();
        final Object fetcherResult = queryFetcher.get(env);
        assertThat(fetcherResult).isEqualTo(products);
    }

    private MirrorDataFetcherClient createClient() {
        return new MirrorDataFetcherClient(this.host, this.client, this.mirrorConfig,
            new KnownTypeResolver<>(JsonNode.class, this.mapper));
    }

    @Data
    @Builder
    private static class PurchaseList {
        private String purchaseId;
        private List<Integer> productIds;
    }

    @Data
    @Builder
    private static class Purchase {
        private String purchaseId;
        private int productId;
        private int amount;
        private Price price;
    }

    @Data
    @Builder
    private static class Product {
        private int productId;
        private List<Integer> prices;
    }

    @Data
    @Builder
    private static class Price {
        private String currencyId;
        private double value;
    }

    @Data
    @Builder
    private static class Currency {
        private String currencyId;
        private String currency;
        private double rate;
    }
}
