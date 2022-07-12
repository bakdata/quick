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

package com.bakdata.quick.gateway.security;

import static io.micronaut.http.HttpRequest.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bakdata.quick.common.api.model.ErrorMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponseProvider;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.Optional;
import javax.inject.Inject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "quick.kafka.bootstrap-server", value = "dummy:1234")
@Property(name = "quick.kafka.schema-registry-url", value = "http://dummy")
@Property(name = "micronaut.security.enabled", value = "true")
class ApiKeyTest {
    private static final String SECURE_PATH = "control/definition";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Client(value = "/")
    @Inject
    private RxHttpClient client;

    @Test
    void shouldUnauthorizedWhenAnonymousClient() {
        final BlockingHttpClient httpClient = this.client.toBlocking();
        final MutableHttpRequest<String> request = POST(SECURE_PATH, "");
        final Throwable exception = assertThrows(HttpClientResponseException.class, () -> httpClient.exchange(request));
        assertThat(exception.getMessage()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldAuthenticateWithClientCredentialsFlow() {

        final BlockingHttpClient httpClient = this.client.toBlocking();
        final HttpRequest<?> request = HttpRequest.create(HttpMethod.POST, "/control/schema")
            .header("X-API-Key", "test_key");

        assertThatExceptionOfType(HttpClientResponseException.class)
            .isThrownBy(() -> httpClient.retrieve(request))
            .isInstanceOfSatisfying(HttpClientResponseException.class, ex ->
                assertThat(this.extractErrorMessage(ex))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("type", "errors/clientError")
                    .hasFieldOrPropertyWithValue("title", "Bad Request")
                    .extracting(ErrorMessage::getDetail, InstanceOfAssertFactories.STRING)
                    .startsWith("Required Body [schema] not specified"));
    }

    private Optional<ErrorMessage> extractErrorMessage(final HttpResponseProvider ex) {
        try {
            return Optional
                .ofNullable(this.objectMapper.readValue((String) ex.getResponse().body(), ErrorMessage.class));
        } catch (final JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
