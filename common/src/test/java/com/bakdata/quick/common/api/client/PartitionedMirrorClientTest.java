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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.mirror.HeaderConstants;
import com.bakdata.quick.common.api.client.mirror.MirrorClient;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManagerWithFallback;
import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.api.client.routing.DefaultPartitionFinder;
import com.bakdata.quick.common.api.client.routing.PartitionRouter;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.resolver.StringResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
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
    private final DefaultPartitionFinder mockPartitionFinder = mock(DefaultPartitionFinder.class);

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        // First response: mapping from partition to host for initializing PartitionRouter
        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host, 1, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        final MirrorRequestManagerWithFallback requestManager =
            new MirrorRequestManagerWithFallback(this.client, this.mirrorHost);

        final PartitionRouter<String> partitionRouter =
            new PartitionRouter<>(this.client, this.mirrorHost, newStringData().getSerde(),
                this.mockPartitionFinder,
                requestManager);

        this.stringMirrorClient =
            new PartitionedMirrorClient<>(this.client, new StringResolver(), requestManager, partitionRouter);
    }

    @Test
    void shouldThrowExceptionIfNoMappingUpdate() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(1);

        this.stringMirrorClient.fetchValue(DEFAULT_KEY);

        this.server.enqueue(new MockResponse().setBody(body));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(2);

        assertThatThrownBy(() -> this.stringMirrorClient.fetchValue(DEFAULT_KEY))
            .isInstanceOf(MirrorException.class)
            .hasMessage("No MirrorHost found for partition: 2");

    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithAdditionalReplica() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS));

        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host, 1, this.host,
            2, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(1);

        final String response1 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);

        this.server.enqueue(new MockResponse().setBody(body));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(2);
        final String response2 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);

        assertThat(Objects.requireNonNull(response1)).isEqualTo(TEST_MESSAGE);
        assertThat(Objects.requireNonNull(response2)).isEqualTo(TEST_MESSAGE);
    }

    @Test
    void shouldReadHeaderAndUpdateRouterWithFewerReplicas() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>(TEST_MESSAGE));
        this.server.enqueue(new MockResponse().setBody(body));
        this.server.enqueue(new MockResponse().setBody(body).setHeader(
            HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER, HeaderConstants.HEADER_EXISTS));

        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(1);

        final String response1 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);

        this.server.enqueue(new MockResponse().setBody(body));

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(0);
        final String response2 = this.stringMirrorClient.fetchValue(DEFAULT_KEY);

        assertThat(Objects.requireNonNull(response1)).isEqualTo(TEST_MESSAGE);
        assertThat(Objects.requireNonNull(response2)).isEqualTo(TEST_MESSAGE);

        when(this.mockPartitionFinder.getForSerializedKey(any(), anyInt())).thenReturn(1);

        assertThatThrownBy(() -> this.stringMirrorClient.fetchValue(DEFAULT_KEY))
            .isInstanceOf(MirrorException.class)
            .hasMessage("No MirrorHost found for partition: 1");
    }
}

