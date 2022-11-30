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

package com.bakdata.quick.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.TestTopicRegistryClient;
import com.bakdata.quick.common.api.client.mirror.TopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.gateway.GraphQLTestUtil.TestClientSupplier;
import com.bakdata.quick.gateway.directives.QuickDirectiveWiring;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveWiring;
import com.bakdata.quick.gateway.fetcher.ClientSupplier;
import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.bakdata.quick.gateway.fetcher.FetcherFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class GraphQLQueryExecutionTest {
    private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {};
    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "execution");
    private final TopicRegistryClient registryClient = new TestTopicRegistryClient();
    private final ObjectMapper mapper = new ObjectMapper();

    GraphQLQueryExecutionTest() {
        this.registerTopics();
    }

    @Test
    void shouldExecuteDefinitionWithSingleFieldAndObjectName(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();

        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);
        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("url-topic");
        when(dataFetcherClient.fetchResult("test")).thenAnswer(invocation -> "test-url");

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();

        final Map<String, Map<String, Object>> data = executionResult.getData();
        assertThat(data.get("getURL"))
            .isNotNull()
            .containsEntry("url", "test-url");
    }

    @Test
    void shouldExecuteQueryWithSingleField(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);
        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("purchase-topic");
        final long productId = 123L;
        final Purchase purchase = Purchase.builder().purchaseId("test").amount(5).productId(productId).build();
        when(dataFetcherClient.fetchResult("test")).thenAnswer(invocation -> purchase);

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();

        final Map<String, Map<String, Object>> data = executionResult.getData();
        assertThat(data.get("findPurchase"))
            .isNotNull()
            .containsEntry("purchaseId", "test")
            .containsEntry("amount", 5)
            .containsEntry("productId", productId);
    }

    @Test
    void shouldExecuteRange(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<Integer, ?> dataFetcherClient = testClientSupplier.getClient("user-request-range");

        final List<?> userRequests = List.of(
            UserRequest.builder().userId(1).timestamp(1).requests(5).build(),
            UserRequest.builder().userId(1).timestamp(2).requests(10).build(),
            UserRequest.builder().userId(1).timestamp(3).requests(8).build()
        );

        when(dataFetcherClient.fetchRange(1, "1", "3")).thenAnswer(invocation -> userRequests);

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();

        final Map<String, List<Map<String, Object>>> data = executionResult.getData();
        assertThat(data.get("userRequests"))
            .isNotNull()
            .hasSize(3)
            .satisfies(userRequest -> assertThat(userRequest.get(0).get("requests")).isEqualTo(5))
            .satisfies(userRequest -> assertThat(userRequest.get(1).get("requests")).isEqualTo(10))
            .satisfies(userRequest -> assertThat(userRequest.get(2).get("requests")).isEqualTo(8));
    }

    @Test
    void shouldExecuteQueryWithSingleFieldAndModification(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> purchaseClient = testClientSupplier.getClient("purchase-topic");
        final DataFetcherClient<Long, ?> productClient = testClientSupplier.getClient("product-topic");

        final long productId = 123L;
        final Map<String, Object> purchase1 = this.mapper.convertValue(
            Purchase.builder().purchaseId("purchase1").amount(5).productId(productId).build(),
            OBJECT_TYPE_REFERENCE
        );

        final Product product1 = Product.builder()
            .productId(productId)
            .name("product-name")
            .price(Price.builder().total(5).build())
            .build();

        when(purchaseClient.fetchResult("purchase1")).thenAnswer(invocation -> purchase1);
        when(productClient.fetchResult(productId)).thenAnswer(invocation -> product1);

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();

        final Map<String, Map<String, Object>> data = executionResult.getData();
        assertThat(data.get("findPurchase"))
            .satisfies(purchase -> assertThat(purchase)
                .containsEntry("purchaseId", "purchase1")
                .containsEntry("productId", productId)
                .extractingByKey("product")
                .isNotNull()
            );
    }

    @Test
    void shouldExecuteListQueryWithSingleFieldAndModification(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> purchaseClient = testClientSupplier.getClient("purchase-topic");
        final DataFetcherClient<Long, ?> productClient = testClientSupplier.getClient("product-topic");

        final long productId1 = 123L;
        final long productId2 = 456L;

        final List<?> purchases = List.of(
            this.mapper.convertValue(
                Purchase.builder().purchaseId("purchase1").amount(5).productId(productId1).build(),
                OBJECT_TYPE_REFERENCE
            ),
            this.mapper.convertValue(
                Purchase.builder().purchaseId("purchase2").amount(1).productId(productId2).build(),
                OBJECT_TYPE_REFERENCE
            )
        );

        final Product product1 = Product.builder()
            .productId(productId1)
            .name("product-name")
            .price(Price.builder().total(5).build())
            .build();

        final Product product2 = Product.builder()
            .productId(productId2)
            .name("product-name2")
            .price(Price.builder().total(1).build())
            .build();

        when(purchaseClient.fetchList()).thenAnswer(invocation -> purchases);
        when(productClient.fetchResult(productId1)).thenAnswer(invocation -> product1);
        when(productClient.fetchResult(productId2)).thenAnswer(invocation -> product2);

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();

        final Map<String, List<Map<String, Object>>> data = executionResult.getData();
        assertThat(data.get("findPurchases"))
            .isNotNull()
            .hasSize(2)
            .anySatisfy(purchase ->
                assertThat(purchase)
                    .containsEntry("purchaseId", "purchase1")
                    .containsEntry("productId", productId1)
                    .extractingByKey("product")
                    .isNotNull()
            )
            .anySatisfy(purchase ->
                assertThat(purchase)
                    .containsEntry("purchaseId", "purchase2")
                    .containsEntry("productId", productId2)
                    .extractingByKey("product")
                    .isNotNull()
            );
    }

    @Test
    void shouldExecuteQueryAllWithPrimitiveType(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("url-topic");
        when(dataFetcherClient.fetchList()).thenAnswer(invocation -> List.of("1", "2", "3"));

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();
        final Map<String, List<String>> data = executionResult.getData();
        assertThat(data.get("getURL"))
            .isNotNull()
            .containsExactly("1", "2", "3");
    }

    @Test
    void shouldExecuteQueryWithListArgumentTypeId(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("url-topic");
        when(dataFetcherClient.fetchResults(List.of("1", "2", "3"))).thenAnswer(invocation -> List.of("1", "2", "3"));

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();
        final Map<String, List<String>> data = executionResult.getData();
        assertThat(data.get("getURL"))
            .isNotNull()
            .containsExactly("1", "2", "3");
    }

    @Test
    void shouldExecuteQueryWithListArgumentTypeInt(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<Integer, ?> dataFetcherClient = testClientSupplier.getClient("url-topic");

        when(dataFetcherClient.fetchResults(List.of(1, 2, 3))).thenAnswer(invocation -> List.of("1", "2", "3"));

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();
        final Map<String, List<String>> data = executionResult.getData();
        assertThat(data.get("getURL"))
            .isNotNull()
            .containsExactly("1", "2", "3");
    }

    @Test
    void shouldExecuteQueryWithPrimitiveType(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("url-topic");
        when(dataFetcherClient.fetchResult("test")).thenAnswer(invocation -> "url");

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));

        assertThat(executionResult.getErrors()).isEmpty();
        final Map<String, String> data = executionResult.getData();
        assertThat(data.get("getURL"))
            .isNotNull()
            .isEqualTo("url");
    }

    @Test
    void shouldThrowErrorForNonNullableField() throws IOException {
        final String name = "shouldExecuteQueryWithSingleField";
        final Path schemaPath = workingDirectory.resolve(name + ".graphql");
        final Path queryPath = workingDirectory.resolve(name + "Query.graphql");

        final TestClientSupplier testClientSupplier = new TestClientSupplier();
        final GraphQL graphQL = this.getGraphQL(schemaPath, testClientSupplier);

        final DataFetcherClient<String, ?> dataFetcherClient = testClientSupplier.getClient("purchase-topic");

        final Purchase purchase = Purchase.builder().purchaseId("test").amount(5).productId(null).build();
        when(dataFetcherClient.fetchResult("test")).thenAnswer(invocation -> purchase);

        final ExecutionResult executionResult = graphQL.execute(Files.readString(queryPath));
        assertThat(executionResult.getErrors())
            .hasSize(1)
            .first()
            .satisfies(error -> {
                assertThat(error.getMessage()).startsWith(
                    "The field at path '/findPurchase/productId' was declared as a non null type");
                assertThat(error.getPath()).containsExactly("findPurchase", "productId");
            });
    }

    private GraphQL getGraphQL(final Path schemaPath, final ClientSupplier clientSupplier) throws IOException {
        final KafkaConfig kafkaConfig = new KafkaConfig("dummy", "dummy");

        final TopicTypeService topicTypeService = mock(TopicTypeService.class);

        final ConversionProvider conversionProvider = mock(ConversionProvider.class);

        final FetcherFactory fetcherFactory = new FetcherFactory(kafkaConfig, this.mapper, topicTypeService,
            clientSupplier, conversionProvider);

        final QuickDirectiveWiring topicDirectiveWiring = new TopicDirectiveWiring(fetcherFactory);
        final GraphQLSchemaGenerator graphQLSchemaGenerator =
            new GraphQLSchemaGenerator(List.of(topicDirectiveWiring), Collections.emptyList(),
                Collections.emptyList());

        final GraphQLSchema schema = graphQLSchemaGenerator.create(Files.readString(schemaPath));
        return GraphQL.newGraphQL(schema).build();
    }

    private void registerTopics() {
        this.registryClient.register(
            "purchase-topic",
            new TopicData("purchase-topic", TopicWriteType.MUTABLE, QuickTopicType.STRING, QuickTopicType.AVRO,
                "")
        ).blockingAwait();

        this.registryClient.register(
            "product-topic",
            new TopicData("product-topic", TopicWriteType.MUTABLE, QuickTopicType.LONG, QuickTopicType.PROTOBUF, "")
        ).blockingAwait();

        this.registryClient.register(
            "contract-topic",
            new TopicData("contract-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.AVRO, "")
        ).blockingAwait();

        this.registryClient.register(
            "person-topic",
            new TopicData("person-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.PROTOBUF, "")
        ).blockingAwait();

        this.registryClient.register(
            "url-topic",
            new TopicData("url-topic", TopicWriteType.MUTABLE, QuickTopicType.STRING, QuickTopicType.AVRO, "")
        ).blockingAwait();

        this.registryClient.register(
            "user-request-range",
            new TopicData("user-request-range", TopicWriteType.MUTABLE, QuickTopicType.STRING, QuickTopicType.AVRO,
                "")
        ).blockingAwait();

        this.registryClient.register(
            "info-topic",
            new TopicData("info-topic", TopicWriteType.MUTABLE, QuickTopicType.INTEGER, QuickTopicType.AVRO,
                "")
        ).blockingAwait();
    }

    @Value
    @Builder
    private static class Purchase {
        String purchaseId;
        Long productId;
        int amount;
    }

    @Value
    @Builder
    private static class Product {
        Long productId;
        String name;
        String description;
        Price price;
    }

    @Value
    @Builder
    private static class Price {
        double total;
        String currency;
    }

    @Value
    @Builder
    private static class UserRequest {
        int userId;
        int timestamp;
        int requests;
    }
}
