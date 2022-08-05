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
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private static TopicData createTopicData() {
        return new TopicData(DEFAULT_TOPIC, TopicWriteType.IMMUTABLE, QuickTopicType.LONG, QuickTopicType.STRING, null);
    }

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, host, 2, host));
        // first request sets the mapping with partitions 1 and 2
        this.server.enqueue(new MockResponse().setBody(routerBody));
        this.topicDataClient = new PartitionedMirrorClient<>(DEFAULT_TOPIC, mirrorHost, client, Serdes.String(),
            new KnownTypeResolver<>(TopicData.class, this.mapper), new TestPartitionFinder(List.of(2, 3)));
    }

    /**
     * 1) We start with the following mapping: 1->host, 2->host. This is the mapping the router is initialised with.
     * It is fetched by the first server.enqueue in @BeforeEach.
     * 2) When we make a call to this.topicDataClient.fetchValue(), we'll make a request in
     * DefaultMirrorRequestManager (DMRM).
     * Since we create a mocked response with a header, the control statement at the line 71 of DMRM will be called
     * and the header will be set in the ResponseWrapper.
     * 3) Because of this, this.updateRouterInfo(); at 90 of PartitionedMirrorClient will be called.
     * 4) Eventually, we will get a new mapping: 1->host, 2->host, 3->host.
     * 5) Now, in the first call to this.topicDataClient.fetchValue, the returned partition is 2.
     * If we make a consecutive call to this.topicDataClient.fetchValue, the returned partition will be 3.
     * Why do we get 2 and then 3? This is a way of functioning of the custom PartitioningRouter
     * that was made for the purpose of the test, see PartitionFinderForUpdateMappingTest,
     * 6) If the info had not been updated, we would have received IllegalStateException because
     * the partitionToMirrorHost would not have access to the key=3.
     *
     * @throws JsonProcessingException json processing exception
     */
    @Test
    void shouldReadHeaderAndUpdateRouter() throws JsonProcessingException {
        final TopicData topicData = createTopicData();
        final String body = TestUtils.generateBody(topicData);

        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.getCacheMissHeaderName(), HeaderConstants.getCacheMissHeaderValue()));

        final String routerBody = TestUtils.generateBodyForRouterWith(Map.of(1, host, 2, host, 3, host));
        this.server.enqueue(new MockResponse().setBody(routerBody));
        final TopicData topic = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        this.server.enqueue(new MockResponse().setBody(body));
        final TopicData topic2 = this.topicDataClient.fetchValue(DEFAULT_TOPIC);
        assertThat(Objects.requireNonNull(topic).getName()).isEqualTo(DEFAULT_TOPIC);
        assertThat(Objects.requireNonNull(topic2).getName()).isEqualTo(DEFAULT_TOPIC);
    }
}

