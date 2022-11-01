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

package com.bakdata.quick.manager.security;

import static io.micronaut.http.HttpRequest.DELETE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.manager.topic.TopicService;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "micronaut.security.enabled", value = "true")
class ApiKeyTest {

    private static final String TOPIC = "test-topic";
    public static final String REQUEST_ID = "request123";
    private static final String SECURE_PATH = String.format("/topic/%s", TOPIC);
    @Inject
    TopicService topicService;
    @Client(value = "/")
    @Inject
    private RxHttpClient client;

    @Test
    void shouldUnauthorizedWhenApiKeyNotSetInHeader() {
        final BlockingHttpClient httpClient = this.client.toBlocking();
        final MutableHttpRequest<?> request = DELETE(SECURE_PATH);
        final Throwable exception = assertThrows(HttpClientResponseException.class, () -> httpClient.exchange(request));
        assertThat(exception.getMessage()).isEqualTo("Client '/': Unauthorized");
    }

    @Test
    void shouldAuthorizedWhenApiKeyExists() {
        when(this.topicService.deleteTopic(TOPIC, REQUEST_ID)).thenReturn(Completable.complete());

        final HttpStatus status = this.client
            .toBlocking()
            .exchange(DELETE(SECURE_PATH).header("X-API-Key", "test_key"))
            .getStatus();

        assertThat((CharSequence) status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldAuthorizedWhenApiKeyExistsAndHeaderKeyCaseInsensitive() {
        when(this.topicService.deleteTopic(TOPIC, REQUEST_ID)).thenReturn(Completable.complete());

        final HttpStatus status = this.client
            .toBlocking()
            .exchange(DELETE(SECURE_PATH).header("x-api-key", "test_key"))
            .getStatus();

        assertThat((CharSequence) status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldUnauthorizedWhenApiKeyIsNotValid() {
        final BlockingHttpClient httpClient = this.client.toBlocking();
        final MutableHttpRequest<?> request = DELETE(SECURE_PATH).header("X-API-Key", "wrong_key");
        final Throwable exception = assertThrows(HttpClientResponseException.class, () -> httpClient.exchange(request));
        assertThat(exception.getMessage()).isEqualTo("Client '/': Unauthorized");
    }

    @Test
    void shouldUnauthorizedWhenApiKeyHeaderKeyIsWrong() {
        final BlockingHttpClient httpClient = this.client.toBlocking();
        final MutableHttpRequest<?> request = DELETE(SECURE_PATH).header("WRONG-API-Key", "test_key");
        final Throwable exception = assertThrows(HttpClientResponseException.class, () -> httpClient.exchange(request));
        assertThat(exception.getMessage()).isEqualTo("Client '/': Unauthorized");
    }

    @MockBean(TopicService.class)
    TopicService topicService() {
        return mock(TopicService.class);
    }

}
