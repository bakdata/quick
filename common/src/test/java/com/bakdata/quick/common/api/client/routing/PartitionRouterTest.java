package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.testutils.TestUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Partition Router.
 */
@MicronautTest
public class PartitionRouterTest {

    @Test
    void shouldBeInitializedWithTwoDifferentHosts() {

        final Map<Integer, String> elements = Map.of(1, "1", 2, "2");
        final Router<String> partitionRouter = new PartitionRouter<>(Serdes.String(), "dummy",
                TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.getAllHosts()).hasSize(2);
    }

    @Test
    void shouldReturnCorrectHostForGivenPartition() {

        final Map<Integer, String> elements = Map.of(1, "1", 2, "2");
        final Router<String> partitionRouter = new PartitionRouter<>(Serdes.String(), "dummy",
                TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.getHost("abc").getHost()).isEqualTo("1");
    }

}
