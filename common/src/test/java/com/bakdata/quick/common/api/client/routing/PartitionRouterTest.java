package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.testutils.TestUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class PartitionRouterTest {

    private final MockWebServer server = new MockWebServer();


    @Test
    void shouldBeInitializedWithTwoDifferentHosts() throws InterruptedException {

        Map<Integer, String> elements = Map.of(1, "1", 2, "2");
        Router<String> partitionRouter = new PartitionRouter<>(Serdes.String(), "dummy",
                TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.getAllHosts()).hasSize(2);

        RecordedRequest request = server.takeRequest();
        assertEquals("/streams/partitions", request.getPath());
    }

    @Test
    void shouldReturnCorrectHostForGivenPartition() {

        Map<Integer, String> elements = Map.of(1, "1", 2, "2");
        Router<String> partitionRouter = new PartitionRouter<>(Serdes.String(), "dummy",
                TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.getHost("abc").getHost()).isEqualTo("1");
    }

}
