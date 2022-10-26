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

package com.bakdata.quick.mirror.topology.consumer;

import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.range.KeySelector;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Repartitioned;

/**
 * Implements the stream consume logic from the input topic.
 */
public class MirrorStreamConsumer implements StreamConsumer {
    @Override
    public <K, V, R> KStream<R, V> consume(final MirrorContext<K, V> mirrorContext) {
        final StreamsBuilder streamsBuilder = mirrorContext.getStreamsBuilder();
        final Serde<K> keySerDe = mirrorContext.getKeySerde();
        final Serde<V> valueSerDe = mirrorContext.getValueSerde();
        final QuickTopologyData<K, V> quickTopologyData = mirrorContext.getQuickTopologyData();
        final ParsedSchema parsedSchema = mirrorContext.getTopicData().getValueData().getParsedSchema();
        final String rangeKey = mirrorContext.getRangeIndexProperties().getRangeKey();
        final KStream<K, V> stream =
            streamsBuilder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
        if (rangeKey == null || parsedSchema == null) {
            return (KStream<R, V>) stream;
        } else {
            final KeySelector<V> keySelector = KeySelector.create(mirrorContext, parsedSchema, rangeKey);
            final QuickData<R> repartitionedKeyData = keySelector.getRepartitionedKeyData();
            final Serde<R> serde = repartitionedKeyData.getSerde();
            return stream.selectKey((key, value) -> keySelector.<R>getRangeKeyValue(rangeKey, value))
                .repartition(Repartitioned.with(serde, valueSerDe));
        }
    }
}
