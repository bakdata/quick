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

import static com.bakdata.quick.common.api.client.TestUtils.mockResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManager;
import com.bakdata.quick.common.api.client.mirror.ResponseWrapper;
import com.bakdata.quick.common.api.client.mirror.StreamsStateHost;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.junit.jupiter.api.Test;

/**
 * Test for Partition Router.
 */
class PartitionRouterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.objectMapper, new OkHttpClient());
    private final DefaultPartitionFinder partitionFinder = mock(DefaultPartitionFinder.class);
    private final MirrorRequestManager mockRequestManager = mock(MirrorRequestManager.class);

    @Test
    void shouldReturnCorrectHostForGivenPartition() throws JsonProcessingException {
        final Map<Integer, String> multiReplica = Map.of(0, "123.456.789.000", 1, "000.987.654.321");
        final String body = this.objectMapper.writeValueAsString(multiReplica);

        final String key = "abc";
        final String key2 = "def";
        final byte[] serializedKey1 = new StringSerde().serializer().serialize("test-topic", key);
        final byte[] serializedKey2 = new StringSerde().serializer().serialize("test-topic", key2);

        when(this.partitionFinder.getForSerializedKey(eq(serializedKey1), eq(2))).thenReturn(0);
        when(this.partitionFinder.getForSerializedKey(eq(serializedKey2), eq(2))).thenReturn(1);

        final MirrorHost serviceName = new MirrorHost("test-topic", new MirrorConfig());
        final StreamsStateHost streamsStateHost = StreamsStateHost.fromMirrorHost(serviceName);
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse(body));
        when(this.mockRequestManager.makeRequest(eq(streamsStateHost.getPartitionToHostUrl()))).thenReturn(response);

        final Router<String> partitionRouter = new PartitionRouter<>(this.client, serviceName, new StringSerde(),
            this.partitionFinder,
            this.mockRequestManager);

        final String host1 = partitionRouter.findHost(key).getTopic();
        final String host2 = partitionRouter.findHost(key2).getTopic();
        assertThat(host1).isEqualTo("123.456.789.000");
        assertThat(host2).isEqualTo("000.987.654.321");
    }

    @Test
    void shouldReturnSingleHostWhenTheyAreEqualAndTwoIfTheyDiffer() throws JsonProcessingException {
        final Map<Integer, String> singleReplica = Map.of(0, "123.456.789.000", 1, "123.456.789.000");
        final String body = this.objectMapper.writeValueAsString(singleReplica);
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse(body));

        final MirrorHost serviceName = new MirrorHost("test-topic", new MirrorConfig());
        final StreamsStateHost streamsStateHost = StreamsStateHost.fromMirrorHost(serviceName);
        when(this.mockRequestManager.makeRequest(eq(streamsStateHost.getPartitionToHostUrl()))).thenReturn(response);

        final Router<String> partitionRouter = new PartitionRouter<>(this.client, serviceName, new StringSerde(),
            this.partitionFinder,
            this.mockRequestManager);

        final List<MirrorHost> allHosts = partitionRouter.getAllHosts();

        assertThat(allHosts).hasSize(1);

        final Map<Integer, String> multiReplica = Map.of(0, "123.456.789.000", 1, "000.987.654.321");
        final String updateValue = this.objectMapper.writeValueAsString(multiReplica);
        final ResponseWrapper updateResponse = ResponseWrapper.fromResponse(mockResponse(updateValue));
        when(this.mockRequestManager.makeRequest(eq(streamsStateHost.getPartitionToHostUrl()))).thenReturn(
            updateResponse);

        partitionRouter.updateRoutingInfo();

        final List<MirrorHost> updatedHosts = partitionRouter.getAllHosts();
        assertThat(updatedHosts).hasSize(2);
    }

    @Test
    void shouldThrowExceptionWhenPartitionToMirrorHostIsEmpty() {
        final ResponseWrapper response = ResponseWrapper.fromResponse(mockResponse());

        final MirrorHost serviceName = new MirrorHost("test-topic", new MirrorConfig());
        final StreamsStateHost streamsStateHost = StreamsStateHost.fromMirrorHost(serviceName);
        when(this.mockRequestManager.makeRequest(eq(streamsStateHost.getPartitionToHostUrl()))).thenReturn(response);

        final Router<String> partitionRouter = new PartitionRouter<>(this.client, serviceName, new StringSerde(),
            this.partitionFinder,
            this.mockRequestManager);

        assertThatThrownBy(partitionRouter::getAllHosts)
            .isInstanceOf(MirrorException.class)
            .hasMessageContaining("Partition to MirrorHost mapping is empty.");
    }
}
