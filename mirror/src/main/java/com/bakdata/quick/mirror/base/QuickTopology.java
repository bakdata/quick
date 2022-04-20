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

package com.bakdata.quick.mirror.base;

import com.bakdata.quick.common.type.QuickTopicData;
import java.util.List;

/**
 * Kafka Streams topology.
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class QuickTopology<K, V> {
    private final QuickTopologyData<K, V> data;

    protected QuickTopology(final QuickTopologyData<K, V> topologyData) {
        this.data = topologyData;
    }

    protected List<String> getInputTopics() {
        return this.data.getInputTopics();
    }

    protected QuickTopicData<K, V> getTopicData() {
        return this.data.getTopicData();
    }
}
