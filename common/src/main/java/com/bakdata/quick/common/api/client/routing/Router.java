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

import com.bakdata.quick.common.api.client.mirror.MirrorHost;
import java.util.List;

/**
 * Strategy for finding a host that keeps the information about a specific key, i.e., a partition of a topic
 *
 * @param <K> key type
 */
public interface Router<K> {

    /**
     * Fetches the host that corresponds to the partition for a given key.
     *
     * @param key key
     * @return a replica-host where partitions for a given key are stored.
     */
    MirrorHost findHost(K key);

    /**
     * Retrieves all mirror hosts in a specific app.
     *
     * @return a list of hosts
     */
    List<MirrorHost> getAllHosts();

    /**
     * Updates the current routing information with a provided routing information.
     */
    void updateRoutingInfo();
}
