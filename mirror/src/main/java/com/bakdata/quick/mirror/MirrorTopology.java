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

import com.bakdata.quick.mirror.base.QuickTopology;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.range.MirrorRangeProcessor;
import com.bakdata.quick.mirror.retention.RetentionMirrorProcessor;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Duration;
import java.util.Objects;
import lombok.Builder;
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
 * Kafka Streams topology for mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MirrorTopology<K, V> extends QuickTopology<K, V> {
    public static final String RETENTION_SINK = "same-topic-sink";
    private static final String PROCESSOR_NAME = "mirror-processor";
    private static final String RANGE_PROCESSOR_NAME = "mirror-range-processor";

    private final String storeName;
    private final String rangeStoreName;
    private final String retentionStoreName;

    private final boolean isPoint;
    @Nullable
    private final String rangeField;
    @Nullable
    private final Duration retentionTime;
    private final StoreType storeType;

    /**
     * Constructor used by builder.
     */
    @Builder
    public MirrorTopology(final QuickTopologyData<K, V> topologyData, final String storeName,
        final String rangeStoreName, final boolean isPoint, @Nullable final String rangeField,
        @Nullable final Duration retentionTime,
        final String retentionStoreName, final StoreType storeType) {
        super(topologyData);
        this.storeName = storeName;
        this.rangeStoreName = rangeStoreName;
        this.isPoint = isPoint;
        this.rangeField = rangeField;
        this.retentionTime = retentionTime;
        this.retentionStoreName = retentionStoreName;
        this.storeType = storeType;
    }

    /**
     * Creates a new mirror topology.
     */
    public Topology createTopology(final StreamsBuilder builder) {
        final Serde<K> keySerDe = this.getTopicData().getKeyData().getSerde();
        final Serde<V> valueSerDe = this.getTopicData().getValueData().getSerde();

        final KStream<K, V> stream = builder.stream(this.getInputTopics(), Consumed.with(keySerDe, valueSerDe));

        // if the user set a retention time, we use a special mirror processor that schedules a job for it
        if (this.retentionTime == null) {
            if (this.isPoint) {
                builder.addStateStore(
                    Stores.keyValueStoreBuilder(this.createStore(this.storeName), keySerDe, valueSerDe));
                stream.process(() -> new MirrorProcessor<>(this.storeName), Named.as(PROCESSOR_NAME), this.storeName);
            }
            if (this.rangeField != null) {
                // key serde is string because the store saves zero padded range index string as keys
                builder.addStateStore(
                    Stores.keyValueStoreBuilder(this.createStore(this.rangeStoreName), Serdes.String(), valueSerDe));
                stream.process(
                    () -> new MirrorRangeProcessor<>(this.rangeStoreName, Objects.requireNonNull(this.rangeField)),
                    Named.as(RANGE_PROCESSOR_NAME), this.rangeStoreName);
            }
            return builder.build();
        } else {
            // key serde is long because the store saves the timestamps as keys
            // value serde is key serde because the store save the keys as values
            final KeyValueBytesStoreSupplier retentionStore = Stores.inMemoryKeyValueStore(this.retentionStoreName);
            final long millisRetentionTime = this.retentionTime.toMillis();
            builder.addStateStore(Stores.keyValueStoreBuilder(retentionStore, Serdes.Long(), keySerDe));
            stream.process(() -> new RetentionMirrorProcessor<>(
                    this.storeName,
                    millisRetentionTime,
                    this.retentionStoreName
                ),
                Named.as(PROCESSOR_NAME),
                this.storeName,
                this.retentionStoreName
            );
            final Topology topology = builder.build();
            topology.addSink(
                RETENTION_SINK,
                this.getTopicData().getName(),
                Serdes.Long().serializer(),
                keySerDe.serializer(),
                PROCESSOR_NAME
            );
            return topology;
        }
    }

    private KeyValueBytesStoreSupplier createStore(final String name) {
        switch (this.storeType) {
            case ROCKSDB:
                return Stores.persistentKeyValueStore(name);
            case INMEMORY:
            default:
                return Stores.inMemoryKeyValueStore(name);
        }
    }
}
