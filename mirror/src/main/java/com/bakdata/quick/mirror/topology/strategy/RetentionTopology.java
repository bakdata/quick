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

import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.retention.RetentionMirrorProcessor;
import com.bakdata.quick.mirror.service.context.RangeIndexProperties;
import com.bakdata.quick.mirror.service.context.RetentionTimeProperties;
import com.bakdata.quick.mirror.topology.TopologyContext;
import java.util.Objects;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

/**
 * Creates a retention topology.
 */
public class RetentionTopology implements TopologyStrategy {
    public static final String RETENTION_SINK = "same-topic-sink";
    private static final String PROCESSOR_NAME = "mirror-processor";

    /**
     * Validates if retention time topology should be crated.
     */
    @Override
    public <K, V> boolean isApplicable(final TopologyContext<K, V> topologyContext) {
        final RetentionTimeProperties retentionTimeProperties = topologyContext.getRetentionTimeProperties();
        final RangeIndexProperties rangeIndexProperties = topologyContext.getRangeIndexProperties();
        return retentionTimeProperties.isEnabled() && !rangeIndexProperties.isEnabled();
    }

    /**
     * Creates retention time topology.
     */
    @Override
    public <K, V> void create(final TopologyContext<K, V> topologyContext) {
        final RetentionTimeProperties retentionTimeProperties = topologyContext.getRetentionTimeProperties();
        final Serde<K> keySerDe = topologyContext.getKeySerde();
        final Serde<V> valueSerDe = topologyContext.getValueSerde();

        final StreamsBuilder builder = topologyContext.getStreamsBuilder();
        final String retentionStoreName = retentionTimeProperties.getStoreName();
        final KeyValueBytesStoreSupplier retentionStore = Stores.inMemoryKeyValueStore(retentionStoreName);

        // key serde is long because the store saves the timestamps as keys
        // value serde is key serde because the store save the keys as values
        builder.addStateStore(Stores.keyValueStoreBuilder(retentionStore, Serdes.Long(), keySerDe));

        final QuickTopologyData<K, V> quickTopologyData = topologyContext.getQuickTopologyData();
        final KStream<K, V> stream =
            builder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));

        final String storeName = retentionTimeProperties.getStoreName();
        final long millisRetentionTime = Objects.requireNonNull(retentionTimeProperties.getRetentionTime()).toMillis();
        stream.process(() -> new RetentionMirrorProcessor<>(
                storeName,
                millisRetentionTime,
                retentionStoreName
            ),
            Named.as(PROCESSOR_NAME),
            storeName,
            retentionStoreName
        );
    }

    /**
     * Builds the retention time topology and adds the sink to the topology.
     */
    @Override
    public <K, V> Topology extendTopology(final TopologyContext<K, V> topologyContext, final Topology topology) {
        final Serde<K> keySerDe = topologyContext.getKeySerde();
        topology.addSink(
            RETENTION_SINK,
            topologyContext.getTopicData().getName(),
            Serdes.Long().serializer(),
            keySerDe.serializer(),
            PROCESSOR_NAME
        );
        return topology;
    }
}
