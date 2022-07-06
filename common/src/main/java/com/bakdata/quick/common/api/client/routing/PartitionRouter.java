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

package com.bakdata.quick.common.api.client.routing;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import org.apache.kafka.common.serialization.Serde;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A router that leverages the fact that a mirror knows which partitions it stores
 * (it has a mapping between a partitions and a host),
 * and thus can use this information to introduce routing based on the specific partition mapping.
 *
 * @param <K> the type of key
 */
public class PartitionRouter<K> implements Router<K> {

    private final String topic;
    private final Serde<K> keySerde;
    private final PartitionFinder partitionFinder;
    private final Map<Integer, String> partitionToHost;

    private final Map<Integer, MirrorHost> partitionToMirrorHost = new HashMap<>();

    /**
     * A constructor with the default partitioner that is retrieved from a static method.
     *
     * @param keySerde serializer for the key
     * @param topic the name of the corresponding topic
     * @param partitionFinder strategy for finding partitions
     * @param partitionToHost partition to host mapping
     */
    public PartitionRouter(final Serde<K> keySerde, final String topic,
                           final PartitionFinder partitionFinder, final Map<Integer, String> partitionToHost) {
        this.topic = topic;
        this.keySerde = keySerde;
        this.partitionFinder = partitionFinder;
        this.partitionToHost = partitionToHost;
        convertHostStringToMirrorHost();
    }

    private void convertHostStringToMirrorHost() {
        for (final Map.Entry<Integer, String> entry : this.partitionToHost.entrySet()) {
            final MirrorHost host = new MirrorHost(entry.getValue(), MirrorConfig.directAccess());
            // partition -> host
            partitionToMirrorHost.put(entry.getKey(), host);
        }
    }

    @Override
    public MirrorHost getHost(final K key) {
        final byte[] serializedKey = this.keySerde.serializer().serialize(topic, key);
        final int partition = partitionFinder.getForSerializedKey(serializedKey, this.partitionToMirrorHost.size());
        if (partitionToMirrorHost.isEmpty() || !partitionToMirrorHost.containsKey(partition)) {
            throw new IllegalStateException("Router has not been initialized properly.");
        }
        return this.partitionToMirrorHost.get(partition);
    }

    @Override
    public List<MirrorHost> getAllHosts() {
        if (partitionToMirrorHost.isEmpty()) {
            throw new IllegalStateException("Router has not been initialized properly.");
        }
        return new ArrayList<>(partitionToMirrorHost.values());
    }
}
