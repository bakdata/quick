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

package com.bakdata.quick.mirror.range;

import com.bakdata.quick.mirror.range.indexer.RangeIndexer;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Processor for filling up Kafka state store for range queries.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class MirrorRangeProcessor<K, V> implements Processor<K, V, Void, Void> {
    private final String storeName;
    private final RangeIndexer<? super K, ? super V> defaultRangeIndexer;
    @Nullable
    private KeyValueStore<String, V> store = null;

    /**
     * Standard constructor.
     *
     * @param storeName The name of the range store
     * @param defaultRangeIndexer Creates and prepares the range index format
     */
    public MirrorRangeProcessor(final String storeName, final RangeIndexer<? super K, ? super V> defaultRangeIndexer) {
        this.storeName = storeName;
        this.defaultRangeIndexer = defaultRangeIndexer;
    }

    @Override
    public void init(final ProcessorContext<Void, Void> context) {
        this.store = context.getStateStore(this.storeName);
    }

    @Override
    public void process(final Record<K, V> record) {
        final K key = record.key();
        final V value = record.value();

        if (this.store == null) {
            throw new IllegalStateException("MirrorProcessor was not initialized.");
        }

        if (value == null) {
            log.warn("Skipping range index creation for key {}. Because the value is null.", key);
            return;
        }

        final String rangeIndex = this.defaultRangeIndexer.createIndex(key, value);

        log.debug("creating range index: {}", rangeIndex);

        this.store.put(rangeIndex, value);
    }
}
