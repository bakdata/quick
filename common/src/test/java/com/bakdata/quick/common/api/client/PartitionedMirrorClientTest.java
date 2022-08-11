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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.testutils.TestPartitionFinder;
import com.bakdata.quick.common.testutils.TestUtils;
import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class PartitionedMirrorClientTest {

    private static final String DEFAULT_TOPIC = "dummy";

    private final MockWebServer server = new MockWebServer();

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private MirrorClient<String, TopicData> topicDataClient;
    private final Queue<Integer> partitionQueue = new ArrayDeque<>();
    private final TestPartitionFinder partitionFinder = new TestPartitionFinder(partitionQueue);

    private static TopicData createTopicData() {
        return new TopicData(DEFAULT_TOPIC, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // First response: mapping from partition to host for initializing PartitionRouter
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, host, 2, "local:1234"));
        this.server.enqueue(new MockResponse().setBody(routerBody));
        this.topicDataClient = new PartitionedMirrorClient<>(DEFAULT_TOPIC, mirrorHost, client, Serdes.String(),
            new KnownTypeResolver<>(TopicData.class, this.mapper), partitionFinder);
    }

    @Test
    void shouldThrowExceptionIfNoMappingUpdate() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror.
        // The X-Cache-Update Header is not set, and thus there will be no mapping update.
        final TopicData topicData = createTopicData();
        final String body = TestUtils.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // The current mapping is 1->host, 2->host, so we will successfully return a host.
        this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        // Third response: A dummy message for yet another call to Mirror.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(3);
        // Since there was no mapping update, an exception will be thrown.
        // There is no host for partition=3
        assertThatThrownBy(() -> this.topicDataClient.fetchValue(DEFAULT_TOPIC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No MirrorHost found for partition: 3");

    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithAdditionalReplica() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror contains the X-Cache-Update header.
        // The header is set to simulate a mapping change from partition to host.
        final TopicData topicData = createTopicData();
        final String body = TestUtils.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.getCacheMissHeaderName(), HeaderConstants.getCacheMissHeaderValue()));
        // Third response: A new mapping for PartitionedMirrorClient
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, host, 2, host, 3, host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // In the current scenario, we will make two calls to Mirror when we fetch a value.
        // The first is done to get a value (second response), second to get mapping for updating PartitionRouter
        // (third response)
        final TopicData topic = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        // Fourth response: A dummy message for yet another call to Mirror is needed
        // to test whether the mapping has been successfully updated.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(3);
        final TopicData topic2 = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        // If the info had not been updated, we would have received IllegalStateException because
        // the partitionToMirrorHost would not have access to the partition=3.
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo(DEFAULT_TOPIC);
        assertThat(Objects.requireNonNull(topic2).getName()).isEqualTo(DEFAULT_TOPIC);
    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithFewerReplicas() throws JsonProcessingException {
        // Second response: A dummy response for PartitionedMirrorClient from Mirror contains the X-Cache-Update header.
        // The header is set to simulate a mapping change from partition to host.
        final TopicData topicData = createTopicData();
        final String body = TestUtils.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.getCacheMissHeaderName(), HeaderConstants.getCacheMissHeaderValue()));

        // Third response: A new mapping for PartitionedMirrorClient
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        // The PartitionedRouter of PartitionedMirrorClient will get partition=2 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(2);
        // In the current scenario, we will make two calls to Mirror when we fetch a value.
        // The first is done to get a value (second response), second to get mapping for updating PartitionRouter
        // (third response)
        final TopicData topic = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        // Fourth response: A dummy message for yet another call to Mirror is needed
        // to test whether the mapping has been successfully updated.
        this.server.enqueue(new MockResponse().setBody(body));
        // The PartitionedRouter of PartitionedMirrorClient will get partition=3 from
        // the underlying PartitionFinder in PartitionRouter.findHost function.
        this.partitionFinder.enqueue(1);
        final TopicData topic2 = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        // If the info had not been updated, we would have received IllegalStateException because
        // the partitionToMirrorHost would not have access to the partition=3.
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo(DEFAULT_TOPIC);
        assertThat(Objects.requireNonNull(topic2).getName()).isEqualTo(DEFAULT_TOPIC);

        this.partitionFinder.enqueue(2);
        // Since there was no mapping update, an exception will be thrown.
        // There is no host for partition=3
        assertThatThrownBy(() -> this.topicDataClient.fetchValue(DEFAULT_TOPIC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No MirrorHost found for partition: 2");

    }

}

