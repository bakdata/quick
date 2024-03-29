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
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

/**
 * Strategy for different topologies.
 */
public interface TopologyStrategy {

    /**
     * Validates if a topology strategy should be created or not.
     */
    <K, V> boolean isApplicable(final MirrorContext<K, V> mirrorContext);

    /**
     * Includes the implementation logic of the topology.
     */
    <K, V> void create(final MirrorContext<?, V> mirrorContext, final KStream<K, V> stream);

    /**
     * Adds source, processor, or sink to the {@link Topology}. The default returns the passed in {@link Topology}.
     */
    default <K, V> Topology extendTopology(final MirrorContext<K, V> mirrorContext, final Topology topology) {
        return topology;
    }

    /**
     * Creates a {@link  KeyValueBytesStoreSupplier} based on the {@link StoreType}.
     */
    default KeyValueBytesStoreSupplier createStore(final String name, final StoreType storeType) {
        switch (storeType) {
            case ROCKSDB:
                return Stores.persistentKeyValueStore(name);
            case INMEMORY:
            default:
                return Stores.inMemoryKeyValueStore(name);
        }
    }
}
