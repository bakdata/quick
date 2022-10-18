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

import com.bakdata.quick.common.type.QuickTopicData;
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
 *
 * @param <K> Type of the key
 * @param <V> Type of the value
 */
public class PointTopology<K, V> implements TopologyStrategy {
    private final TopologyContext<K, V> topologyContext;
    private static final String PROCESSOR_NAME = "mirror-processor";

    /**
     * Default constructor.
     */
    public PointTopology(final TopologyContext<K, V> topologyContext) {
        this.topologyContext = topologyContext;
    }

    /**
     * Always apply point query index.
     */
    @Override
    public boolean apply() {
        return true;
    }

    /**
     * Creates a topology for point queries.
     */
    @Override
    public void create() {
        final StreamsBuilder streamsBuilder = this.topologyContext.getStreamsBuilder();

        final QuickTopologyData<K, V> quickTopologyData = this.topologyContext.getQuickTopologyData();
        final QuickTopicData<K, V> topicData = quickTopologyData.getTopicData();

        final Serde<K> keySerDe = topicData.getKeyData().getSerde();
        final Serde<V> valueSerDe = topicData.getValueData().getSerde();

        final String storeName = this.topologyContext.getPointStoreName();
        final StoreType storeType = this.topologyContext.getStoreType();
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(this.createStore(storeName, storeType), keySerDe, valueSerDe));
        final KStream<K, V> stream =
            streamsBuilder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        stream.process(() -> new MirrorProcessor<>(storeName), Named.as(PROCESSOR_NAME), storeName);
        this.topologyContext.setStreamsBuilder(streamsBuilder);
    }
}
