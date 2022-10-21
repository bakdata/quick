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

package com.bakdata.quick.mirror.topology.strategy;

import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.point.MirrorProcessor;
import com.bakdata.quick.mirror.topology.TopologyContext;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.state.Stores;

/**
 * Creates an index for point queries.
 */
public class PointTopology implements TopologyStrategy {
    private static final String PROCESSOR_NAME = "mirror-processor";

    /**
     * Always apply point query index.
     */
    @Override
    public <K, V> boolean isApplicable(final TopologyContext<K, V> topologyContext) {
        return true;
    }

    /**
     * Creates a topology for point queries.
     */
    @Override
    public <K, V> void create(final TopologyContext<K, V> topologyContext) {
        final StreamsBuilder streamsBuilder = topologyContext.getStreamsBuilder();

        final Serde<K> keySerDe = topologyContext.getKeySerde();
        final Serde<V> valueSerDe = topologyContext.getValueSerde();

        final String storeName = topologyContext.getPointStoreName();
        final StoreType storeType = topologyContext.getStoreType();
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(this.createStore(storeName, storeType), keySerDe, valueSerDe));

        final QuickTopologyData<K, V> quickTopologyData = topologyContext.getQuickTopologyData();
        final KStream<K, V> stream =
            streamsBuilder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        stream.process(() -> new MirrorProcessor<>(storeName), Named.as(PROCESSOR_NAME), storeName);
    }
}