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

package com.bakdata.quick.mirror.retention;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Duration;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Processor that ensures the retention time in state stores.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class RetentionMirrorProcessor<K, V> implements Processor<K, V> {
    private static final Duration INTERVAL = Duration.ofSeconds(5);
    private final String storeName;
    private final long retentionTime; // in millis
    private final String timestampStoreName;

    @Nullable
    private KeyValueStore<K, V> store = null;
    @Nullable
    private KeyValueStore<Long, K> timestampStore = null;


    /**
     * Default constructor.
     *
     * @param storeName          store to check retention time in
     * @param retentionTime      retention time of keys in store in milli seconds
     * @param timestampStoreName store name storing how long a key is kept
     */
    public RetentionMirrorProcessor(final String storeName, final long retentionTime, final String timestampStoreName) {
        this.storeName = storeName;
        this.retentionTime = retentionTime;
        this.timestampStoreName = timestampStoreName;
    }

    @Override
    public void init(final ProcessorContext context) {
        this.store = context.getStateStore(this.storeName);
        this.timestampStore = context.getStateStore(this.timestampStoreName);

        final Punctuator punctuator = new RetentionPunctuator<>(
            this.retentionTime,
            this.timestampStore,
            context
        );
        context.schedule(INTERVAL, PunctuationType.WALL_CLOCK_TIME, punctuator);
    }

    @Override
    public void process(final K key, final V value) {
        if (this.store == null || this.timestampStore == null) {
            throw new IllegalStateException("RetentionTimeProcessor was not intiialized.");
        }

        if (value == null) {
            this.store.delete(key);
        } else {
            // TODO use composite key to circumvent collisions for keys inserted at the same time
            this.timestampStore.put(System.currentTimeMillis(), key);
            this.store.put(key, value);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
