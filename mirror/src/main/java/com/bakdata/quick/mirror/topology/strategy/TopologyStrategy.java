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
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

/**
 * Strategy for different topologies.
 */
public interface TopologyStrategy {

    /**
     * Validates if a topology strategy should be created or not.
     */
    boolean applicable();

    /**
     * Includes the implementation logic of the topology.
     */
    void create();

    /**
     * Builds the {@link StreamsBuilder} into a {@link Topology}.
     */
    default Topology buildTopology(final StreamsBuilder streamsBuilder) {
        return streamsBuilder.build();
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
