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

import static com.bakdata.quick.mirror.topology.strategy.RetentionTopology.RETENTION_SINK;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Periodic operation removing keys whose retention time is up.
 *
 * @param <K> key type
 */
@Slf4j
public class RetentionPunctuator<K> implements Punctuator {

    private final long retentionTime;
    private final KeyValueStore<Long, K> timestampStore;
    private final ProcessorContext<Void, Void> context;
    private long from = 0;

    /**
     * Default constructor.
     */
    public RetentionPunctuator(final long retentionTime, final KeyValueStore<Long, K> timestampStore,
        final ProcessorContext<Void, Void> context) {
        this.retentionTime = retentionTime;
        this.timestampStore = timestampStore;
        this.context = context;
    }

    @Override
    public void punctuate(final long timestamp) {
        final long to = timestamp - this.retentionTime;
        try (final KeyValueIterator<Long, K> range = this.timestampStore.range(this.from, to)) {

            range.forEachRemaining(keyValue -> {
                log.debug("Retention time for key {} expired", keyValue.key);
                this.context.forward(new Record(keyValue.value, null, timestamp), RETENTION_SINK);
                this.timestampStore.delete(keyValue.key);
            });
        }
        this.from = to;
    }
}
