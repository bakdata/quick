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

package com.bakdata.quick.mirror.context;

import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.state.HostInfo;

/**
 * A context containing all the necessary information for creating the Mirror topology context.
 *
 * @param <K> The type of the key
 * @param <V> The type of the value
 */
@Builder(toBuilder = true)
@Value
public class MirrorContext<K, V> {
    // Write data
    @Default
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    QuickTopologyData<K, V> quickTopologyData;
    String pointStoreName;
    RangeIndexProperties rangeIndexProperties;
    RetentionTimeProperties retentionTimeProperties;
    StoreType storeType;
    boolean isCleanup;
    FieldTypeExtractor fieldTypeExtractor;
    FieldValueExtractor<V> fieldValueExtractor;

    // Read data
    KafkaStreams streams;
    HostInfo hostInfo;

    /**
     * Gets the list of input topics.
     */
    public List<String> getInputTopics() {
        return this.quickTopologyData.getInputTopics();
    }

    /**
     * Gets the {@link QuickTopicData}.
     */
    public QuickTopicData<K, V> getTopicData() {
        return this.quickTopologyData.getTopicData();
    }

    /**
     * Returns the SerDe of the key.
     */
    public Serde<K> getKeySerde() {
        return this.getTopicData().getKeyData().getSerde();
    }

    /**
     * Returns the SerDe of the value.
     */
    public Serde<V> getValueSerde() {
        return this.getTopicData().getValueData().getSerde();
    }
}
