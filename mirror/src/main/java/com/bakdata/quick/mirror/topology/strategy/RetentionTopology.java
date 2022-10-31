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

import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.context.RangeIndexProperties;
import com.bakdata.quick.mirror.context.RetentionTimeProperties;
import com.bakdata.quick.mirror.retention.RetentionMirrorProcessor;
import java.util.Objects;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
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
    public <K, V> boolean isApplicable(final MirrorContext<K, V> mirrorContext) {
        final RetentionTimeProperties retentionTimeProperties = mirrorContext.getRetentionTimeProperties();
        final RangeIndexProperties rangeIndexProperties = mirrorContext.getRangeIndexProperties();
        return retentionTimeProperties.isEnabled() && !rangeIndexProperties.isEnabled();
    }

    /**
     * Creates retention time topology.
     */
    @Override
    public <K, V> void create(final MirrorContext<?, V> mirrorContext, final KStream<K, V> stream) {
        final StreamsBuilder builder = mirrorContext.getStreamsBuilder();
        final RetentionTimeProperties retentionTimeProperties = mirrorContext.getRetentionTimeProperties();
        final String retentionStoreName = retentionTimeProperties.getStoreName();
        final KeyValueBytesStoreSupplier retentionStore = Stores.inMemoryKeyValueStore(retentionStoreName);

        // key serde is long because the store saves the timestamps as keys
        // value serde is key serde because the store save the keys as values
        final Serde<Long> keySerde = Serdes.Long();
        final Serde<?> valueSerde = mirrorContext.getKeySerde();
        builder.addStateStore(Stores.keyValueStoreBuilder(retentionStore, keySerde, valueSerde));

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
    public <K, V> Topology extendTopology(final MirrorContext<K, V> mirrorContext, final Topology topology) {
        final Serde<K> keySerde = mirrorContext.getKeySerde();
        topology.addSink(
            RETENTION_SINK,
            mirrorContext.getTopicName(),
            Serdes.Long().serializer(),
            keySerde.serializer(),
            PROCESSOR_NAME
        );
        return topology;
    }
}
