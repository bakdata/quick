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

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.range.MirrorRangeProcessor;
import com.bakdata.quick.mirror.range.indexer.NoOpRangeIndexer;
import com.bakdata.quick.mirror.range.indexer.RangeIndexer;
import com.bakdata.quick.mirror.range.indexer.WriteRangeIndexer;
import com.bakdata.quick.mirror.topology.TopologyContext;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.state.Stores;

/**
 * Crates the range topology.
 */
@Slf4j
public class RangeTopology implements TopologyStrategy {
    private static final String RANGE_PROCESSOR_NAME = "mirror-range-processor";

    /**
     * Validates if the range topology should be built.
     */
    @Override
    public <K, V> boolean isApplicable(final TopologyContext<K, V> topologyContext) {
        return topologyContext.getRangeIndexProperties().isEnabled()
            && !topologyContext.getRetentionTimeProperties().isEnabled();
    }

    /**
     * Creates a range topology.
     */
    @Override
    public <K, V> void create(final TopologyContext<K, V> topologyContext) {
        this.createRangeTopology(topologyContext);
    }

    private <K, V> void createRangeTopology(final TopologyContext<K, V> topologyContext) {
        final StreamsBuilder streamsBuilder = topologyContext.getStreamsBuilder();
        final Serde<K> keySerDe = topologyContext.getKeySerde();
        final Serde<V> valueSerDe = topologyContext.getValueSerde();

        final String rangeStoreName = topologyContext.getRangeIndexProperties().getStoreName();
        final StoreType storeType = topologyContext.getStoreType();

        // key serde is string because the store saves zero padded range index string as keys
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(this.createStore(rangeStoreName, storeType), Serdes.String(), valueSerDe));

        final RangeIndexer<K, V> rangeIndexer = getRangeIndexer(topologyContext);

        final KStream<K, V> stream =
            streamsBuilder.stream(topologyContext.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        stream.process(() -> new MirrorRangeProcessor<>(rangeStoreName, rangeIndexer),
            Named.as(RANGE_PROCESSOR_NAME), rangeStoreName);
    }

    private static <K, V> RangeIndexer<K, V> getRangeIndexer(final TopologyContext<K, V> topologyContext) {
        final ParsedSchema parsedSchema = topologyContext.getTopicData().getValueData().getParsedSchema();
        if (parsedSchema == null) {
            final boolean isCleanup = topologyContext.isCleanup();
            log.debug("Parsed schema is null and cleanup flag is set to {}.", isCleanup);
            if (isCleanup) {
                return new NoOpRangeIndexer<>();
            }
            throw new MirrorTopologyException("Could not get the parsed schema.");
        } else {
            final String rangeField =
                Objects.requireNonNull(topologyContext.getRangeIndexProperties().getRangeField());
            log.debug("Setting up default range indexer.");
            return WriteRangeIndexer.create(parsedSchema, rangeField);
        }
    }
}
