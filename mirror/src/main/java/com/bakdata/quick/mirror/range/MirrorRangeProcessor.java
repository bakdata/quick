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

import static com.bakdata.quick.mirror.range.RangeUtils.createRangeIndex;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

@Slf4j
public class MirrorRangeProcessor<K, V> implements Processor<K, V, Void, Void> {
    private final String storeName;
    private final String rangeField;
    @Nullable
    private KeyValueStore<String, V> store = null;

    public MirrorRangeProcessor(final String storeName, final String rangeField) {
        this.storeName = storeName;
        this.rangeField = rangeField;
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

        final String rangeIndex = createRangeIndex(key, value, this.rangeField);
        log.debug("crating range index: {}", rangeIndex);

        this.store.put(rangeIndex, value);
    }
}