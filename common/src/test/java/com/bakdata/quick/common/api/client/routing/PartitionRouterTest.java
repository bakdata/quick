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

import com.bakdata.quick.common.testutils.TestUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.Map;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

/**
 * Test for Partition Router.
 */
@MicronautTest
public class PartitionRouterTest {


    @Test
    void shouldReturnSingleHostWhenTheyAreEqualAndTwoIfTheyDiffer() {
        final Map<Integer, String> elements = Map.of(1, "host1", 2, "host1");
        final Router<String> partitionRouter =
            new PartitionRouter<>(Serdes.String(), "dummy", TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.getAllHosts()).hasSize(1);
        partitionRouter.updateRoutingInfo(Map.of(1, "host1", 2, "host2"));
        assertThat(partitionRouter.getAllHosts()).hasSize(2);
    }

    @Test
    void shouldReturnCorrectHostForGivenPartition() {
        final Map<Integer, String> elements = Map.of(1, "host1", 2, "host2");
        final Router<String> partitionRouter =
            new PartitionRouter<>(Serdes.String(), "dummy", TestUtils.getMockPartitionFinder(), elements);
        assertThat(partitionRouter.findHost("abc").getHost()).isEqualTo("host1");
    }
}
