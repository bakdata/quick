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

/**
 * Test for the Fallback mechanism of the PartitionedMirrorClient.
 */
public class FallbackMechanismTest {

    private static final String DEFAULT_TOPIC = "dummy";

    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private MirrorClient<String, TopicData> topicDataClient;
    private final Queue<Integer> partitionQueue = new ArrayDeque<>();
    private final TestPartitionFinder partitionFinder = new TestPartitionFinder(this.partitionQueue);

    private static TopicData createTopicData() {
        return new TopicData(DEFAULT_TOPIC, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // First response: mapping from partition to host for initializing PartitionRouter
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, this.host,
            2, "unavailableHost:1234"));
        this.server.enqueue(new MockResponse().setBody(routerBody));
        this.topicDataClient = new PartitionedMirrorClient<>(this.mirrorHost, this.client,
            Serdes.String(), new KnownTypeResolver<>(TopicData.class, this.mapper), this.partitionFinder);
    }

    @Test
    void shouldUpdateMappingBecauseOfFallback() throws JsonProcessingException {
        // Second response: A dummy response for the FallBack Service
        // The first returned partition is 2. However, as the mapping indicates,
        // the host for this partition points to an address that can never be reached.
        // The unavailability of the host triggers the fallback routine, which makes the call to the functional host.
        final TopicData topicData = createTopicData();
        final String body = TestUtils.generateBody(topicData);
        this.server.enqueue(new MockResponse().setBody(body));

        // Third response: The fallback mechanism sets the update header behind the scenes,
        // so the mocked response doesn't have to contain it.
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, this.host));
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
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo(DEFAULT_TOPIC);
        assertThat(Objects.requireNonNull(topic2).getName()).isEqualTo(DEFAULT_TOPIC);
    }
}
