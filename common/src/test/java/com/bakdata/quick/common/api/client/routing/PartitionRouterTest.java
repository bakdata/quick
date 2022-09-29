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

package com.bakdata.quick.common.api.client.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManager;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManagerWithFallback;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.junit.jupiter.api.Test;

/**
 * Test for Partition Router.
 */
class PartitionRouterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();
    private final HttpClient client = new HttpClient(this.objectMapper, new OkHttpClient());
    private final String host = String.format("localhost:%s", this.server.getPort());
    private final DefaultPartitionFinder partitionFinder = mock(DefaultPartitionFinder.class);


    @Test
    void shouldReturnSingleHostWhenTheyAreEqualAndTwoIfTheyDiffer() throws JsonProcessingException {
        final Map<Integer, String> singleReplica = Map.of(0, this.host, 1, this.host);
        final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
        final MirrorRequestManager requestManager = new MirrorRequestManagerWithFallback(this.client, mirrorHost);

        final String value = this.objectMapper.writeValueAsString(singleReplica);
        this.server.enqueue(new MockResponse().setBody(value));
        final Router<String> partitionRouter = new PartitionRouter<>(this.client, mirrorHost, new StringSerde(),
            this.partitionFinder,
            requestManager);

        assertThat(partitionRouter.getAllHosts()).hasSize(1);
        final Map<Integer, String> multiReplica = Map.of(0, this.host, 1, "newlocalhost");
        final String updateValue = this.objectMapper.writeValueAsString(multiReplica);
        this.server.enqueue(new MockResponse().setBody(updateValue));
        partitionRouter.updateRoutingInfo();
        assertThat(partitionRouter.getAllHosts()).hasSize(2);
    }

    @Test
    void shouldReturnCorrectHostForGivenPartition() throws JsonProcessingException {
        final Map<Integer, String> elements = Map.of(0, "host1", 1, "host2");
        final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
        final MirrorRequestManager requestManager = new MirrorRequestManagerWithFallback(this.client, mirrorHost);
        final String value = this.objectMapper.writeValueAsString(elements);
        this.server.enqueue(new MockResponse().setBody(value));

        final String key = "abc";
        final String key2 = "def";
        final byte[] serializedKey1 = new StringSerde().serializer().serialize("test-topic", key);
        final byte[] serializedKey2 = new StringSerde().serializer().serialize("test-topic", key2);

        when(this.partitionFinder.getForSerializedKey(eq(serializedKey1), eq(2))).thenReturn(0);
        when(this.partitionFinder.getForSerializedKey(eq(serializedKey2), eq(2))).thenReturn(1);

        final Router<String> partitionRouter = new PartitionRouter<>(this.client, mirrorHost, new StringSerde(),
            this.partitionFinder,
            requestManager);
        assertThat(partitionRouter.findHost(key).getTopic()).isEqualTo("host1");
        assertThat(partitionRouter.findHost(key2).getTopic()).isEqualTo("host2");
    }
}
