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
import lombok.extern.slf4j.Slf4j;
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
 *
 * @param <K> Type of the key
 * @param <V> Type of the value
 */
@Slf4j
public class RetentionTopology<K, V> implements TopologyStrategy {
    public static final String RETENTION_SINK = "same-topic-sink";
    private static final String PROCESSOR_NAME = "mirror-processor";
    private final TopologyContext<K, V> topologyContext;

    public RetentionTopology(final TopologyContext<K, V> topologyContext) {
        this.topologyContext = topologyContext;
    }

    /**
     * Validates if retention time topology should be crated.
     */
    @Override
    public boolean applicable() {
        final RetentionTimeProperties retentionTimeProperties = this.topologyContext.getRetentionTimeProperties();
        final RangeIndexProperties rangeIndexProperties = this.topologyContext.getRangeIndexProperties();
        return retentionTimeProperties.getRetentionTime() != null && rangeIndexProperties.getRangeField() == null;
    }

    /**
     * Creates retention time topology.
     */
    @Override
    public void create() {
        // key serde is long because the store saves the timestamps as keys
        // value serde is key serde because the store save the keys as values
        final RetentionTimeProperties retentionTimeProperties = this.topologyContext.getRetentionTimeProperties();

        this.createRetentionTopology(retentionTimeProperties);
    }

    private void createRetentionTopology(final RetentionTimeProperties retentionTimeProperties) {
        final QuickTopologyData<K, V> quickTopologyData = this.topologyContext.getQuickTopologyData();
        final Serde<K> keySerDe = this.topologyContext.getKeySerde();
        final Serde<V> valueSerDe = this.topologyContext.getValueSerde();

        final StreamsBuilder builder = this.topologyContext.getStreamsBuilder();
        final String retentionStoreName = retentionTimeProperties.getStoreName();
        final KeyValueBytesStoreSupplier retentionStore = Stores.inMemoryKeyValueStore(retentionStoreName);
        final long millisRetentionTime = Objects.requireNonNull(retentionTimeProperties.getRetentionTime()).toMillis();

        builder.addStateStore(Stores.keyValueStoreBuilder(retentionStore, Serdes.Long(), keySerDe));
        final String storeName = this.topologyContext.getRetentionTimeProperties().getStoreName();

        final KStream<K, V> stream =
            builder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        stream.process(() -> new RetentionMirrorProcessor<>(
                storeName,
                millisRetentionTime,
                retentionStoreName
            ),
            Named.as(PROCESSOR_NAME),
            storeName,
            retentionStoreName
        );

        this.topologyContext.setStreamsBuilder(builder);
    }

    /**
     * Builds the retention time topology and adds the sink to the topology.
     */
    @Override
    public Topology buildTopology(final StreamsBuilder streamsBuilder) {
        final Topology topology = streamsBuilder.build();

        final Serde<K> keySerDe = this.topologyContext.getKeySerde();
        topology.addSink(
            RETENTION_SINK,
            this.topologyContext.getTopicData().getName(),
            Serdes.Long().serializer(),
            keySerDe.serializer(),
            PROCESSOR_NAME
        );
        log.debug("The topology is {}", topology.describe());
        return topology;
    }
}
