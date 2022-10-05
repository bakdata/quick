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

package com.bakdata.quick.common.api.client.mirror;

import static com.bakdata.quick.common.api.client.TestUtils.mockResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicType;
import java.util.List;
import java.util.Objects;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

class TopicRegistryMirrorClientTest {
    private final MirrorHost mirrorHost = new MirrorHost("internal-topic-registry", MirrorConfig.directAccess());
    private final HttpClient mockClient = mock(HttpClient.class);
    private final TypeResolver<TopicData> mockTypeResolver = mock(KnownTypeResolver.class);
    private final MirrorRequestManager mockRequestManager = mock(MirrorRequestManager.class);
    private final MirrorClient<String, TopicData> topicRegistryMirrorClient =
        new DefaultMirrorClient<>(this.mirrorHost,
            this.mockClient,
            this.mockTypeResolver,
            this.mockRequestManager);

    @Test
    void shouldReturnGetTopicDataWithSpecificKey() {
        final TopicData topicData = createTopicData("test-topic");

        final HttpUrl httpUrl = this.mirrorHost.forKey("test-topic");
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(topicData);

        final TopicData topic = this.topicRegistryMirrorClient.fetchValue("test-topic");

        verify(this.mockRequestManager).makeRequest(any());
        verify(this.mockRequestManager).processResponse(any(), any());
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo("test-topic");
    }

    @Test
    void shouldReturnListOfTopicsData() {
        final TopicData topicData = createTopicData("test-topic-1");
        final TopicData topicData2 = createTopicData("test-topic-2");

        final HttpUrl httpUrl = this.mirrorHost.forKeys(List.of("test-topic-1", "test-topic-2"));
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(List.of(topicData, topicData2));

        final List<TopicData> topic = this.topicRegistryMirrorClient.fetchValues(List.of("test-topic-1", "test-topic-2"));

        verify(this.mockRequestManager).makeRequest(any());
        verify(this.mockRequestManager).processResponse(any(), any());
        assertThat(topic).hasSize(2)
            .extracting(TopicData::getName)
            .containsExactly("test-topic-1", "test-topic-2");
    }

    @Test
    void shouldReturnGetAllTopicData() {
        final TopicData topicData = createTopicData("test-topic-1");
        final TopicData topicData2 = createTopicData("test-topic-2");

        final HttpUrl httpUrl = this.mirrorHost.forAll();
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(List.of(topicData, topicData2));

        final List<TopicData> topic = this.topicRegistryMirrorClient.fetchAll();
        verify(this.mockRequestManager).makeRequest(any());
        verify(this.mockRequestManager).processResponse(any(), any());
        assertThat(topic).hasSize(2)
            .extracting(TopicData::getName)
            .containsExactly("test-topic-1", "test-topic-2");
    }

    @Test
    void shouldReturnTrueIfTopicExists() {
        final TopicData topicData = createTopicData("test-topic");

        final HttpUrl httpUrl = this.mirrorHost.forKey("test-topic");
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(topicData);

        final Boolean exists = this.topicRegistryMirrorClient.exists("test-topic");
        assertThat(exists).isTrue();
    }

    @Test
    void shouldEmptyListIfTopicDoesNotExists() {
        final HttpUrl httpUrl = this.mirrorHost.forAll();
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());
        when(this.mockRequestManager.makeRequest(eq(httpUrl))).thenReturn(response);
        when(this.mockRequestManager.processResponse(eq(response), any())).thenReturn(null);

        assertThat(this.topicRegistryMirrorClient.fetchAll()).isEmpty();

        assertThat(this.topicRegistryMirrorClient.fetchValues(List.of("some-topic"))).isEmpty();
        assertThat(this.topicRegistryMirrorClient.fetchRange("some-topic", "1", "4")).isEmpty();
    }

    private static TopicData createTopicData(final String name) {
        return new TopicData(name, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }
}
