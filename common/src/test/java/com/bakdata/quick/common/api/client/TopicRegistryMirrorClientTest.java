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

package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class TopicRegistryMirrorClientTest {
    private final MockWebServer server = new MockWebServer();

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private final TopicData topicData = createTopicData("dummy");
    private final MirrorClient<String, TopicData> topicDataClient =
        new DefaultMirrorClient<>(this.mirrorHost, this.client, new KnownTypeResolver<>(TopicData.class, this.mapper),
                topicData.getName(), topicData.getKeyType());

    private static TopicData createTopicData(final String name) {
        return new TopicData(name, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    @Test
    void shouldReturnGetTopicDataWithSpecificKey() throws JsonProcessingException {

        final TopicData topicData = createTopicData("dummy");

        final String body = this.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body));

        final TopicData topic = this.topicDataClient.fetchValue("dummy");
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo("dummy");
    }

    @Test
    void shouldReturnListOfTopicsData() throws JsonProcessingException {
        final TopicData topicData = createTopicData("dummy");
        final TopicData topicData2 = createTopicData("dummy2");

        final String body = this.generateBody(List.of(topicData, topicData2));
        this.server.enqueue(new MockResponse().setBody(body));

        final List<TopicData> topic = this.topicDataClient.fetchValues(List.of("dummy", "dummy2"));
        assertThat(topic).hasSize(2).extracting(TopicData::getName).containsExactly("dummy", "dummy2");
    }

    @Test
    void shouldReturnGetAllTopicData() throws JsonProcessingException {
        final TopicData topicData = createTopicData("dummy");
        final TopicData topicData2 = createTopicData("dummy2");

        final String body = this.generateBody(List.of(topicData, topicData2));
        this.server.enqueue(new MockResponse().setBody(body));

        final List<TopicData> topic = this.topicDataClient.fetchAll();
        assertThat(topic).hasSize(2).extracting(TopicData::getName).containsExactly("dummy", "dummy2");
    }

    @Test
    void shouldReturnTrueIfTopicDoesNotExist() throws JsonProcessingException {
        final TopicData topicData = createTopicData("dummy");

        final String body = this.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body));

        final Boolean exists = this.topicDataClient.exists("dummy");
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseIfTopicDoesNotExist() {
        this.server.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.getCode()));
        final Boolean exists = this.topicDataClient.exists("dummy");
        assertThat(exists).isFalse();
    }

    private String generateBody(final Object mirrorValue) throws JsonProcessingException {
        return this.mapper.writeValueAsString(new MirrorValue<>(mirrorValue));
    }
}
