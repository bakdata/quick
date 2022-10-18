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
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.range.DefaultRangeIndexer;
import com.bakdata.quick.mirror.range.MirrorRangeProcessor;
import com.bakdata.quick.mirror.range.NoOpRangeIndexer;
import com.bakdata.quick.mirror.range.RangeIndexer;
import com.bakdata.quick.mirror.topology.TopologyContext;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.time.Duration;
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
 *
 * @param <K> The key type
 * @param <V> The value type
 */
@Slf4j
public class RangeTopology<K, V> implements TopologyStrategy {
    private final TopologyContext<K, V> topologyContext;
    private static final String RANGE_PROCESSOR_NAME = "mirror-range-processor";

    public RangeTopology(final TopologyContext<K, V> topologyContext) {
        this.topologyContext = topologyContext;
    }

    /**
     * Validates if the range topology should be built.
     */
    @Override
    public boolean apply() {
        final Duration retentionTime = this.topologyContext.getRetentionTimeProperties().getRetentionTime();
        final String rangeField = this.topologyContext.getRangeIndexProperties().getRangeField();

        return retentionTime == null && rangeField != null;
    }

    /**
     * Creates a range topology.
     */
    @Override
    public void create() {
        final StreamsBuilder builder = this.topologyContext.getStreamsBuilder();

        this.createRangeTopology(builder);
        log.debug("The topology is {}", builder.build().describe());
        this.topologyContext.setStreamsBuilder(builder);
    }

    private void createRangeTopology(final StreamsBuilder streamsBuilder) {
        final Serde<K> keySerDe = this.topologyContext.getKeySerde();
        final Serde<V> valueSerDe = this.topologyContext.getValueSerde();

        final String rangeStoreName = this.topologyContext.getRangeIndexProperties().getStoreName();
        final StoreType storeType = this.topologyContext.getStoreType();

        // key serde is string because the store saves zero padded range index string as keys
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(this.createStore(rangeStoreName, storeType), Serdes.String(), valueSerDe));

        final QuickTopicType keyType = this.topologyContext.getTopicData().getKeyData().getType();
        final ParsedSchema parsedSchema = this.topologyContext.getTopicData().getValueData().getParsedSchema();
        log.debug("keyType is {} and parsedSchema is {}", keyType, parsedSchema);

        final String rangeField =
            Objects.requireNonNull(this.topologyContext.getRangeIndexProperties().getRangeField());
        final RangeIndexer<K, V> rangeIndexer = this.getRangeIndexer(rangeField, parsedSchema);

        final KStream<K, V> stream =
            streamsBuilder.stream(this.topologyContext.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        stream.process(() -> new MirrorRangeProcessor<>(rangeStoreName, rangeIndexer),
            Named.as(RANGE_PROCESSOR_NAME), rangeStoreName);
    }

    private RangeIndexer<K, V> getRangeIndexer(final String rangeField, @Nullable final ParsedSchema parsedSchema) {
        if (parsedSchema == null) {
            final boolean isCleanup = this.topologyContext.isCleanup();
            log.debug("Parsed schema is null and cleanup flag is set to {}.", isCleanup);
            if (isCleanup) {
                return new NoOpRangeIndexer<>();
            }
            throw new MirrorTopologyException("Could not get the parsed schema.");
        } else {
            log.debug("Setting up default range indexer.");
            return DefaultRangeIndexer.createRangeIndexer(parsedSchema, rangeField);
        }
    }
}
