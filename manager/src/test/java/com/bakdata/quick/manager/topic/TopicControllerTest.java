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

package com.bakdata.quick.manager.topic;

import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.manager.GatewaySchema;
import com.bakdata.quick.common.api.model.manager.creation.TopicCreationData;
import com.bakdata.quick.common.type.QuickTopicType;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@MicronautTest
class TopicControllerTest {

    public static final String BASE_PATH = "/topic/{name}";
    private static final String NAME = "test-topic";
    public static final String REQUEST_ID = "request123";
    private static String baseUri = null;

    @Client("/")
    @Inject
    RxHttpClient client;

    @Inject
    TopicService service;

    @BeforeAll
    static void init() {
        baseUri = UriBuilder.of(BASE_PATH)
            .expand(Collections.singletonMap("name", NAME))
            .toString();
        assertEquals("/topic/test-topic", baseUri);
    }

    @Test
    void shouldGetTopicList() {
        final TopicData topic =
            new TopicData(NAME, TopicWriteType.MUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
        when(this.service.getTopicList(REQUEST_ID)).thenReturn(Single.just(List.of(topic)));

        this.client.toBlocking().exchange(GET("/topics"));

        verify(this.service).getTopicList(REQUEST_ID);
    }

    @Test
    void shouldGetTopic() {
        when(this.service.getTopicData(anyString(), REQUEST_ID))
            .thenReturn(Single.just(
                new TopicData(NAME, TopicWriteType.MUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null)));

        this.client.toBlocking().exchange(GET(baseUri));

        verify(this.service).getTopicData(NAME, REQUEST_ID);
    }

    @Test
    void testCreateTopicWhenQueryIsNotDefined() {
        when(this.service.createTopic(anyString(), any(), any(), any(), REQUEST_ID)).thenReturn(Completable.complete());

        final TopicCreationData creationData =
            new TopicCreationData(TopicWriteType.MUTABLE, null, new GatewaySchema("test", "test"), null, true, null);
        this.client.toBlocking().exchange(POST(baseUri, creationData));

        verify(this.service).createTopic(NAME, QuickTopicType.LONG, QuickTopicType.SCHEMA, creationData, REQUEST_ID);
    }

    @Test
    void testCreateTopicWhenQueryIsSet() {
        when(this.service.createTopic(anyString(), any(), any(), any(), REQUEST_ID)).thenReturn(Completable.complete());

        final String uri = UriBuilder.of(baseUri)
            .queryParam("keyType", QuickTopicType.STRING)
            .queryParam("valueType", QuickTopicType.DOUBLE)
            .build()
            .toString();

        final TopicCreationData creationData =
            new TopicCreationData(TopicWriteType.MUTABLE, null, null, null, true, null);
        this.client.toBlocking().exchange(POST(uri, creationData));

        verify(this.service).createTopic(NAME, QuickTopicType.STRING, QuickTopicType.DOUBLE, creationData, REQUEST_ID);
    }

    @Test
    void shouldDeleteTopic() {
        when(this.service.deleteTopic(NAME, REQUEST_ID)).thenReturn(Completable.complete());
        this.client.toBlocking().exchange(DELETE(baseUri));
        verify(this.service).deleteTopic(NAME, REQUEST_ID);
    }

    @MockBean(TopicService.class)
    TopicService topicService() {
        return mock(TopicService.class);
    }
}
