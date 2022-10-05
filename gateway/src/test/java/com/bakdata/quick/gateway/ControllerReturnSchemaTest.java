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

import static com.bakdata.quick.gateway.ControllerUpdateSchemaTest.extractErrorMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bakdata.quick.common.api.client.mirror.TopicRegistryClient;
import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.type.QuickTopicType;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@MicronautTest
class ControllerReturnSchemaTest {

    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema");
    @Inject
    private QuickGraphQLContext context;
    @Inject
    private TopicRegistryClient registryClient;
    @Client("/")
    @Inject
    private RxHttpClient httpClient;

    @BeforeEach
    void setUp() throws IOException {
        this.registryClient.register(
            "purchase-topic",
            new TopicData("purchase-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.AVRO,
                "")
        ).blockingAwait();

        this.registryClient.register(
            "product-topic",
            new TopicData("product-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.PROTOBUF, "")
        ).blockingAwait();

        final Path schemaPath = workingDirectory.resolve("schema.graphql");
        this.context.updateFromSchemaString(Files.readString(schemaPath));
    }

    @Test
    void shouldReturnStringSchema() throws IOException {
        final String expectedProduct = Files.readString(workingDirectory.resolve("product.graphql"));
        final String expectedPurchase = Files.readString(workingDirectory.resolve("purchase.graphql"));

        final HttpRequest<?> productRequest = HttpRequest.create(HttpMethod.GET, "/schema/product");
        final SchemaData productResponse =
            this.httpClient.exchange(productRequest, Argument.of(SchemaData.class))
                .blockingFirst()
                .body();

        assertThat(productResponse)
            .isNotNull()
            .extracting(SchemaData::getSchema).asString()
            .isEqualToIgnoringWhitespace(expectedProduct);

        final HttpRequest<?> purchaseRequest = HttpRequest.create(HttpMethod.GET, "/schema/purchase");
        final SchemaData purchaseResponse =
            this.httpClient.exchange(purchaseRequest, Argument.of(SchemaData.class))
                .blockingFirst()
                .body();

        assertThat(purchaseResponse)
            .isNotNull()
            .extracting(SchemaData::getSchema).asString()
            .isEqualToIgnoringWhitespace(expectedPurchase);
    }

    @Test
    void shouldThrowErrorIfTypeDoesNotExist() {
        final HttpRequest<?> request = HttpRequest.create(HttpMethod.GET, "/schema/nope");
        final BlockingHttpClient blockingHttpClient = this.httpClient.toBlocking();
        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> blockingHttpClient.retrieve(request))
            .isInstanceOfSatisfying(HttpClientResponseException.class, ex ->
                assertThat(extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .startsWith("Type nope does not exist")
            );
    }
}
