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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

@MicronautTest
class ControllerUpdateSchemaTest {
    @Client("/")
    @Inject
    private RxHttpClient httpClient;

    @Inject
    private ApplicationContext context;
    
    static Optional<ErrorMessage> extractErrorMessage(final HttpClientResponseException ex) {
        try {
            return Optional
                .ofNullable(new ObjectMapper().readValue((String) ex.getResponse().body(), ErrorMessage.class));
        } catch (final JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Test
    void returnsErrorForEmptyBody() {
        final HttpRequest<?> httpRequest =
            HttpRequest.create(HttpMethod.POST, "/control/schema");

        final BlockingHttpClient blockingHttpClient = this.httpClient.toBlocking();
        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> blockingHttpClient.retrieve(httpRequest))
            .isInstanceOfSatisfying(HttpClientResponseException.class, ex ->
                assertThat(extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .startsWith("Required Body [schema] not specified")
            );
    }

    @Test
    void returnsErrorForWrongBody() {
        final HttpRequest<?> httpRequest = HttpRequest.create(HttpMethod.POST, "/control/schema")
            .body(new SchemaData("test"));

        final BlockingHttpClient blockingHttpClient = this.httpClient.toBlocking();
        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> blockingHttpClient.retrieve(httpRequest))
            .isInstanceOfSatisfying(HttpClientResponseException.class,
                ex -> assertThat(extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .startsWith("Could not parse GraphQL schema:")
            );
    }
}

