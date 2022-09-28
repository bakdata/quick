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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.mirror.MirrorClient;
import com.bakdata.quick.common.api.client.mirror.MirrorRequestManagerWithFallback;
import com.bakdata.quick.common.api.client.mirror.PartitionedMirrorClient;
import com.bakdata.quick.common.api.client.routing.DefaultPartitionFinder;
import com.bakdata.quick.common.api.client.routing.PartitionRouter;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.StringResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for the Fallback mechanism of the PartitionedMirrorClient.
 */
class FallbackMechanismTest {
    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private MirrorClient<String, String> topicDataClient;
    private final DefaultPartitionFinder mockPartitionFinder = mock(DefaultPartitionFinder.class);

    @BeforeEach
    void initRouterAndMirror() throws JsonProcessingException {
        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host,
            1, "unavailableHost:1234"));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        final MirrorRequestManagerWithFallback requestManager =
            new MirrorRequestManagerWithFallback(this.client, this.mirrorHost);

        final PartitionRouter<String> partitionRouter =
            new PartitionRouter<>(this.client, this.mirrorHost, newStringData().getSerde(),
                this.mockPartitionFinder,
                requestManager);

        this.topicDataClient =
            new PartitionedMirrorClient<>(this.client, new StringResolver(), requestManager, partitionRouter);
    }

    @Test
    void shouldUpdateMappingBecauseOfFallback() throws JsonProcessingException {
        final String body = this.mapper.writeValueAsString(new MirrorValue<>("value1"));
        this.server.enqueue(new MockResponse().setBody(body));

        // Return the available host
        final String routerBody = this.mapper.writeValueAsString(Map.of(0, this.host));
        this.server.enqueue(new MockResponse().setBody(routerBody));

        when(this.mockPartitionFinder.getForSerializedKey(eq("dummy1".getBytes()), eq(2))).thenReturn(1);
        final String value1 = this.topicDataClient.fetchValue("dummy1");

        when(this.mockPartitionFinder.getForSerializedKey(eq("dummy2".getBytes()), eq(2))).thenReturn(0);
        final String body2 = this.mapper.writeValueAsString(new MirrorValue<>("value2"));
        this.server.enqueue(new MockResponse().setBody(body2));
        final String value2 = this.topicDataClient.fetchValue("dummy2");

        assertThat(value1).isEqualTo("value1");
        assertThat(value2).isEqualTo("value2");
    }
}
