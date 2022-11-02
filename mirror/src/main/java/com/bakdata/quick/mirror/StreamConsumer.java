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

package com.bakdata.quick.mirror;

import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.RecordData;
import com.bakdata.quick.mirror.range.KeySelector;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import lombok.Value;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Repartitioned;

/**
 * Contains the logic of consuming the input topic and repartitioning of the topic.
 */
@Singleton
public class StreamConsumer {
    private final SchemaExtractor schemaExtractor;
    private final ConversionProvider conversionProvider;

    @Inject
    public StreamConsumer(final SchemaExtractor schemaExtractor, final ConversionProvider conversionProvider) {
        this.schemaExtractor = schemaExtractor;
        this.conversionProvider = conversionProvider;
    }

    /**
     * Consumes the input topic and does a repartitioning based on the range key.
     */
    // The cast is safe. The generic types of R and K are similar when the rangeKey is null
    @SuppressWarnings("unchecked")
    public <K, R, V> RepartitionedTopologyData<R, V> consume(final QuickTopologyData<K, V> topologyData,
        final StreamsBuilder streamsBuilder,
        @Nullable final String rangeKye) {
        final Serde<K> keySerde = topologyData.getTopicData().getKeyData().getSerde();
        final Serde<V> valueSerde = topologyData.getTopicData().getValueData().getSerde();
        final KStream<K, V> stream =
            streamsBuilder.stream(topologyData.getInputTopics().get(0), Consumed.with(keySerde, valueSerde));
        if (rangeKye != null) {
            final KeySelector<R, V> keySelector = this.getKeySelector(rangeKye, topologyData.getTopicData());
            final RecordData<R, V> recordData =
                new RecordData<>(keySelector.getRepartitionedKeyData(), topologyData.getTopicData()
                    .getValueData());
            final KStream<R, V> repartitionedStream =
                repartitionStream(stream, keySelector, topologyData.getTopicData().getValueData().getSerde(),
                    rangeKye);
            return new RepartitionedTopologyData<>(recordData, repartitionedStream);
        }
        final RecordData<K, V> recordData =
            new RecordData<>(topologyData.getTopicData().getKeyData(), topologyData.getTopicData()
                .getValueData());

        return (RepartitionedTopologyData<R, V>) new RepartitionedTopologyData<>(recordData, stream);
    }

    private static <K, R, V> KStream<R, V> repartitionStream(final KStream<K, V> inputStream,
        final KeySelector<R, V> keySelector,
        final Serde<V> valueSerde, final String rangeKey) {
        final QuickData<R> repartitionedKeyData = keySelector.getRepartitionedKeyData();
        final Serde<R> serde = repartitionedKeyData.getSerde();
        return inputStream.selectKey(
                (key, value) -> keySelector.getRangeKeyValue(Objects.requireNonNull(rangeKey), value))
            .repartition(Repartitioned.with(serde, valueSerde));
    }

    private <K, R, V> KeySelector<R, V> getKeySelector(final String rangeKey, final QuickTopicData<K, V> topicData) {
        final ParsedSchema parsedSchema = Objects.requireNonNull(topicData.getValueData().getParsedSchema());
        final FieldTypeExtractor fieldTypeExtractor = this.schemaExtractor.getFieldTypeExtractor();
        final FieldValueExtractor<V> fieldValueExtractor = this.schemaExtractor.getFieldValueExtractor();
        return KeySelector.create(fieldTypeExtractor, fieldValueExtractor, this.conversionProvider, parsedSchema,
            rangeKey);
    }

    /**
     * Contains the {@link RecordData} and the {@link KStream}.
     */
    @Value
    public static class RepartitionedTopologyData<K, V> {
        RecordData<K, V> recordData;
        KStream<K, V> stream;
    }
}
