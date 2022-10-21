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

import com.bakdata.quick.mirror.topology.strategy.TopologyStrategy;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.Topology;


/**
 * Kafka Streams topology for mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class MirrorTopology<K, V> {

    private final TopologyContext<K, V> topologyContext;

    /**
     * Constructor used by builder.
     */
    public MirrorTopology(final TopologyContext<K, V> topologyContext) {
        this.topologyContext = topologyContext;
    }

    /**
     * Creates a new mirror topology.
     */
    public Topology createTopology() {
        final List<TopologyStrategy> topologyStrategies = TopologyFactory.getStrategies(this.topologyContext);

        for (final TopologyStrategy topologyStrategy : topologyStrategies) {
            topologyStrategy.create(this.topologyContext);
        }

        Topology topology = this.topologyContext.getStreamsBuilder().build();
        for (final TopologyStrategy topologyStrategy : topologyStrategies) {
            topology = topologyStrategy.extendTopology(this.topologyContext, topology);
        }
        log.debug("The topology is {}", topology.describe());
        return topology;
    }
}
