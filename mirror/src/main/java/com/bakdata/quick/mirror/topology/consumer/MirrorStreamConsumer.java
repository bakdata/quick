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

import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.MirrorContext;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;

/**
 * Implements the stream consume logic from the input topic.
 */
public class MirrorStreamConsumer implements StreamConsumer {
    @Override
    public <K, V> KStream<K, V> consume(final MirrorContext<K, V> mirrorContext) {
        final StreamsBuilder streamsBuilder = mirrorContext.getStreamsBuilder();
        final Serde<K> keySerDe = mirrorContext.getKeySerde();
        final Serde<V> valueSerDe = mirrorContext.getValueSerde();
        final QuickTopologyData<K, V> quickTopologyData = mirrorContext.getQuickTopologyData();
        return streamsBuilder.stream(quickTopologyData.getInputTopics(), Consumed.with(keySerDe, valueSerDe));
    }
}
