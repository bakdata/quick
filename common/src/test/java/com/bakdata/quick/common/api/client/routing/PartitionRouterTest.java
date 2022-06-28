package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.StreamsStateHost;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.testutils.TestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class PartitionRouterTest {

    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = new HttpClient(this.mapper, new OkHttpClient());
    private final String host = String.format("%s:%d", this.server.getHostName(), this.server.getPort());
    private final MirrorHost mirrorHost = new MirrorHost(this.host, MirrorConfig.directAccess());
    private final StreamsStateHost streamsStateHost = StreamsStateHost.fromMirrorHost(mirrorHost);
    private final TopicData topicData = TestUtils.createTopicData("dummy");
    private final Serde<String> keySerde = Serdes.String();

    @Test
    void shouldBeInitializedWithTwoDifferentHosts() throws InterruptedException, JsonProcessingException {

        server.enqueue(new MockResponse().setBody(TestUtils.generateBodyForRouter()));
        Router<String> partitionRouter = new PartitionRouter<>(this.client, streamsStateHost, keySerde, topicData.getName());
        assertThat(partitionRouter.getAllHosts()).hasSize(2);

        RecordedRequest request = server.takeRequest();
        assertEquals("/streams/partitions", request.getPath());
    }

    @Test
    void shouldReturnCorrectHostForGivenPartition() {

    }

}
