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

package com.bakdata.quick.mirror.topology;

import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.range.KeySelector;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.topology.consumer.MirrorStreamConsumer;
import com.bakdata.quick.mirror.topology.consumer.StreamConsumer;
import com.bakdata.quick.mirror.topology.strategy.TopologyStrategy;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Repartitioned;


/**
 * Kafka Streams topology for mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class MirrorTopology<K, V, R> {

    private final MirrorContext<K, V> mirrorContext;

    /**
     * Constructor used by builder.
     */
    public MirrorTopology(final MirrorContext<K, V> mirrorContext) {
        this.mirrorContext = mirrorContext;
    }

    /**
     * Creates a new mirror topology.
     */
    public Topology createTopology() {
        final List<TopologyStrategy> topologyStrategies = TopologyFactory.getStrategies(this.mirrorContext);

        final StreamConsumer streamConsumer = new MirrorStreamConsumer();

        final ParsedSchema parsedSchema = this.mirrorContext.getTopicData().getValueData().getParsedSchema();
        final Topology topology;
        final KStream<K, V> stream = streamConsumer.consume(this.mirrorContext);
        final String rangeKey = this.mirrorContext.getRangeKey();
        if (rangeKey != null && parsedSchema != null) {
            final FieldTypeExtractor fieldTypeExtractor = this.mirrorContext.getFieldTypeExtractor();
            final FieldValueExtractor<V> fieldValueExtractor = this.mirrorContext.getFieldValueExtractor();
            final ConversionProvider conversionProvider = this.mirrorContext.getConversionProvider();
            final KeySelector<R, V> keySelector =
                KeySelector.create(fieldTypeExtractor, fieldValueExtractor, conversionProvider, parsedSchema, rangeKey);
            topology = this.createRepartitionTopology(topologyStrategies, rangeKey, stream, keySelector);
        } else {
            topology = applyTopologies(topologyStrategies, stream, this.mirrorContext);
        }
        log.debug("The topology is {}", topology.describe());
        return topology;
    }

    private Topology createRepartitionTopology(final Iterable<? extends TopologyStrategy> topologyStrategies,
        final String rangeKey, final KStream<K, V> stream, final KeySelector<R, V> keySelector) {
        final QuickData<R> repartitionedKeyData = keySelector.getRepartitionedKeyData();
        final Serde<R> serde = repartitionedKeyData.getSerde();
        final KStream<R, V> repartitionStream =
            stream.selectKey((key, value) -> keySelector.getRangeKeyValue(rangeKey, value))
                .repartition(Repartitioned.with(serde, this.mirrorContext.getValueSerde()));
        final MirrorContext<R, V> newMirrorContext = this.mirrorContext.update(repartitionedKeyData);
        return applyTopologies(topologyStrategies, repartitionStream, newMirrorContext);
    }

    private static <V> Topology applyTopologies(final Iterable<? extends TopologyStrategy> topologyStrategies,
        final KStream<?, V> stream,
        final MirrorContext<?, V> mirrorContext) {
        for (final TopologyStrategy topologyStrategy : topologyStrategies) {
            topologyStrategy.create(mirrorContext, stream);
        }
        Topology topology = mirrorContext.getStreamsBuilder().build();
        for (final TopologyStrategy topologyStrategy : topologyStrategies) {
            topology = topologyStrategy.extendTopology(mirrorContext, topology);
        }
        return topology;
    }
}
