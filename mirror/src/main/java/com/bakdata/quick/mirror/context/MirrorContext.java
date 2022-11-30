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

import com.bakdata.quick.mirror.StoreType;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
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
    String topicName;
    IndexInputStream<K, V> indexInputStream;
    String pointStoreName;
    RangeIndexProperties rangeIndexProperties;
    RetentionTimeProperties retentionTimeProperties;
    StoreType storeType;
    boolean isCleanup;
    SchemaExtractor schemaExtractor;
    @Nullable
    String rangeKey;

    // Read data
    KafkaStreams streams;
    HostInfo hostInfo;

    @Nullable
    public ParsedSchema getValueSchema() {
        return this.indexInputStream.getValueData().getParsedSchema();
    }

    /**
     * Returns the SerDe of the key.
     */
    public Serde<K> getKeySerde() {
        return this.indexInputStream.getKeyData().getSerde();
    }

    /**
     * Returns the SerDe of the value.
     */
    public Serde<V> getValueSerde() {
        return this.indexInputStream.getValueData().getSerde();
    }
}
