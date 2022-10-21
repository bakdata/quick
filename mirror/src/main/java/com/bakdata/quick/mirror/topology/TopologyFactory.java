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

package com.bakdata.quick.mirror.topology;

import com.bakdata.quick.mirror.topology.strategy.PointTopology;
import com.bakdata.quick.mirror.topology.strategy.RangeTopology;
import com.bakdata.quick.mirror.topology.strategy.RetentionTopology;
import com.bakdata.quick.mirror.topology.strategy.TopologyStrategy;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple static factory that creates a list of topology strategies.
 */
public final class TopologyFactory {

    private TopologyFactory() {
    }

    /**
     * Returns a list of concrete implementation of {@link TopologyStrategy}.
     */
    public static List<TopologyStrategy> getStrategies(final TopologyContext<?, ?> topologyContext) {
        final List<TopologyStrategy> topologyStrategies = List.of(
            new PointTopology(),
            new RangeTopology(),
            new RetentionTopology()
        );

        return topologyStrategies.stream()
            .filter(topologyStrategy -> topologyStrategy.isApplicable(topologyContext))
            .collect(Collectors.toList());

    }
}
