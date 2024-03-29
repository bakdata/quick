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
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.range.MirrorRangeProcessor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.indexer.RangeIndexer;
import com.bakdata.quick.mirror.range.indexer.WriteRangeIndexer;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
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
    public <K, V> boolean isApplicable(final MirrorContext<K, V> mirrorContext) {
        return mirrorContext.getRangeIndexProperties().isEnabled()
            && !mirrorContext.getRetentionTimeProperties().isEnabled();
    }

    /**
     * Creates a range topology.
     */
    @Override
    public <K, V> void create(final MirrorContext<?, V> mirrorContext, final KStream<K, V> stream) {
        log.info("Setting up the range topology.");
        final StreamsBuilder streamsBuilder = mirrorContext.getStreamsBuilder();
        final Serde<String> keySerde = Serdes.String();
        final Serde<V> valueSerDe = mirrorContext.getValueSerde();
        final String rangeStoreName = mirrorContext.getRangeIndexProperties().getStoreName();
        final StoreType storeType = mirrorContext.getStoreType();

        // key serde is string because the store saves zero padded range index string as keys
        streamsBuilder.addStateStore(
            Stores.keyValueStoreBuilder(this.createStore(rangeStoreName, storeType), keySerde, valueSerDe));

        final RangeIndexer<K, V> rangeIndexer = getRangeIndexer(mirrorContext);

        stream.process(() -> new MirrorRangeProcessor<>(rangeStoreName, rangeIndexer),
            Named.as(RANGE_PROCESSOR_NAME), rangeStoreName);
    }

    private static <K, V> RangeIndexer<K, V> getRangeIndexer(final MirrorContext<?, V> mirrorContext) {
        final ParsedSchema parsedSchema = Objects.requireNonNull(mirrorContext.getValueSchema());
        final String rangeField =
            Objects.requireNonNull(mirrorContext.getRangeIndexProperties().getRangeField());
        log.debug("Setting up default range indexer.");

        final FieldTypeExtractor fieldTypeExtractor = mirrorContext.getSchemaExtractor().getFieldTypeExtractor();
        final FieldValueExtractor<V> fieldValueExtractor =
            mirrorContext.getSchemaExtractor().getFieldValueExtractor();
        return WriteRangeIndexer.create(fieldTypeExtractor, fieldValueExtractor, parsedSchema, rangeField);
    }
}
