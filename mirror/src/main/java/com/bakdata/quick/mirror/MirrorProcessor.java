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

package com.bakdata.quick.mirror;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;


/**
 * Processor for putting filling up Kafka state store.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MirrorProcessor<K, V> implements Processor<K, V> {
    private final String storeName;
    @Nullable
    private KeyValueStore<K, V> store = null;

    public MirrorProcessor(final String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(final ProcessorContext context) {
        this.store = context.getStateStore(this.storeName);
    }

    @Override
    public void process(final K key, final V value) {
        if (this.store == null) {
            throw new IllegalStateException("MirrorProcessor was not initialized.");
        }

        if (value == null) {
            this.store.delete(key);
        } else {
            this.store.put(key, value);
        }
    }

    @Override
    public void close() {
    }
}
