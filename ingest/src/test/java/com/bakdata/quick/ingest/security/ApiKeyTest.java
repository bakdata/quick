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

package com.bakdata.quick.ingest.security;

import static com.bakdata.quick.common.TestTypeUtils.newStringData;
import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.type.registry.QuickTopicTypeService;
import com.bakdata.quick.ingest.service.IngestService;
import com.bakdata.quick.ingest.service.KafkaIngestService;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "micronaut.security.enabled", value = "true")
class ApiKeyTest {

    private static final String TOPIC = "topic";
    private static final String SECURE_PATH = String.format("/%s", TOPIC);

    @Client(value = "/")
    @Inject
    private RxHttpClient client;

    @Inject
    private IngestService ingestService;

    @Inject
    private TopicTypeService typeService;


    @Test
    void shouldUnauthorizedWhenAnonymousClient() {
        final Throwable exception = assertThrows(
            HttpClientResponseException.class,
            () -> this.client.toBlocking().exchange(DELETE(SECURE_PATH))
        );
        assertThat(exception.getMessage()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldAuthenticateWithApiKey() {
        final HttpStatus httpStatus =
            this.callAuthenticatedController("X-API-Key", "test_key");

        assertThat((CharSequence) httpStatus).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotAuthenticateWithInvalidApiKey() {
        final Throwable exception = assertThrows(
            HttpClientResponseException.class,
            () -> this.client
                .toBlocking()
                .exchange(DELETE(SECURE_PATH).header("X-API-Key", "wrong_key"))
        );
        assertThat(exception.getMessage()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldAuthorizedWhenApiKeyExistsAndHeaderKeyCaseInsensitive() {
        final HttpStatus httpStatus =
            this.callAuthenticatedController("x-api-key", "test_key");

        assertThat((CharSequence) httpStatus).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldUnauthorizedWhenApiKeyHeaderKeyIsWrong() {
        final Throwable exception = assertThrows(
            HttpClientResponseException.class,
            () -> this.client
                .toBlocking()
                .exchange(DELETE(SECURE_PATH).header("WRONG-API-Key", "test_key"))
        );
        assertThat(exception.getMessage()).isEqualTo("Unauthorized");
    }

    private HttpStatus callAuthenticatedController(final CharSequence key, final CharSequence value) {
        final KeyValuePair<String, String> pair = new KeyValuePair<>("key", "key");
        final QuickData<String> stringInfo = newStringData();
        final QuickTopicData<String, String> topicInfo =
            new QuickTopicData<>(TOPIC, TopicWriteType.MUTABLE, stringInfo, stringInfo);

        when(this.ingestService.sendData(eq(TOPIC), any())).thenReturn(Completable.complete());
        doReturn(Single.just(topicInfo)).when(this.typeService).getTopicData(TOPIC);

        return this.client.toBlocking().exchange(POST(SECURE_PATH, pair).header(key, value)).getStatus();
    }

    @MockBean(KafkaIngestService.class)
    IngestService ingestService() {
        return mock(IngestService.class);
    }

    @MockBean(QuickTopicTypeService.class)
    TopicTypeService topicTypeService() {
        return mock(TopicTypeService.class);
    }
}
