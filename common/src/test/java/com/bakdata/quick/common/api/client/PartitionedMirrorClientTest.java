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

import static com.bakdata.quick.common.TestTypeUtils.newStringData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.testutils.TestPartitionFinder;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartitionedMirrorClientTest {

    private static final String DEFAULT_KEY = "dummy";
    private static final String TEST_MESSAGE = "test";

    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private MirrorClient<String, String> stringMirrorClient;
    private final Queue<Integer> partitionQueue = new ArrayDeque<>();
    private final TestPartitionFinder partitionFinder = new TestPartitionFinder(this.partitionQueue);
    private final TopicTypeService typeService = mock(TopicTypeService.class);

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // First response: mapping from partition to host for initializing PartitionRouter
        final String routerBody = this.mapper.writeValueAsString(Map.of(1, this.host, 2, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        final QuickTopicData<String, String> topicInfo = newQuickTopicData(newStringData(), new StringResolver());

        this.stringMirrorClient =
            new PartitionedMirrorClient<>(this.mirrorHost, this.client, topicInfo, this.partitionFinder);
    }

    @Test
    void shouldThrowExceptionIfNoMappingUpdate() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror.
        // The X-Cache-Update Header is not set, and thus there will be no mapping update.
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // The current mapping is 1->host, 2->host, so we will successfully return a host.
        this.stringMirrorClient.fetchValue(DEFAULT_KEY);
        // Third response: A dummy message for yet another call to Mirror.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(3);
        // Since there was no mapping update, there will be a call to the fallback service
        assertThatThrownBy(() -> this.stringMirrorClient.fetchValue(DEFAULT_KEY))
            .isInstanceOf(MirrorException.class)
            .hasMessage("No MirrorHost found for partition: 3");

    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithAdditionalReplica() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror contains the X-Cache-Update header.
        // The header is set to simulate a mapping change from partition to host.
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS));
        // Third response: A new mapping for PartitionedMirrorClient
        final String routerBody = this.mapper.writeValueAsString(Map.of(1, this.host, 2, this.host,
            3, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // In the current scenario, we will make two calls to Mirror when we fetch a value.
        // The first is done to get a value (second response), second to get mapping for updating PartitionRouter
        // (third response)
        final String response1 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);
        // Fourth response: A dummy message for yet another call to Mirror is needed
        // to test whether the mapping has been successfully updated.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(3);
        final String response2 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);
        // If the info had not been updated, we would have received IllegalStateException because
        // the partitionToMirrorHost would not have access to the partition=3.
        assertThat(Objects.requireNonNull(response1)).isEqualTo(TEST_MESSAGE);
        assertThat(Objects.requireNonNull(response2)).isEqualTo(TEST_MESSAGE);
    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithFewerReplicas() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror contains the X-Cache-Update header.
        // The header is set to simulate a mapping change from partition to host.
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body));
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS));

        // Third response: A new mapping for PartitionedMirrorClient
        final String routerBody = this.mapper.writeValueAsString(Map.of(1, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // In the current scenario, we will make two calls to Mirror when we fetch a value.
        // The first is done to get a value (second response), second to get mapping for updating PartitionRouter
        // (third response)
        final String response1 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);
        // Fourth response: A dummy message for yet another call to Mirror is needed
        // to test whether the mapping has been successfully updated.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(1);
        final String response2 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);
        // If the info had not been updated, we would have received IllegalStateException because
        // the partitionToMirrorHost would not have access to the partition=3.
        assertThat(Objects.requireNonNull(response1)).isEqualTo(TEST_MESSAGE);
        assertThat(Objects.requireNonNull(response2)).isEqualTo(TEST_MESSAGE);

        this.partitionFinder.enqueue(2);
        // Since there was no mapping update, an exception will be thrown.
        // There is no host for partition=2
        assertThatThrownBy(() -> this.stringMirrorClient.fetchValue(DEFAULT_KEY))
            .isInstanceOf(MirrorException.class)
            .hasMessage("No MirrorHost found for partition: 2");
    }

    private static <K, V> QuickTopicData<K, V> newQuickTopicData(final QuickData<K> keyData,
        final TypeResolver<V> typeResolver) {
        final QuickData<V> valueData = new QuickData<>(QuickTopicType.AVRO, null, typeResolver);
        return new QuickTopicData<>("topic", TopicWriteType.MUTABLE, keyData, valueData);
    }
}

