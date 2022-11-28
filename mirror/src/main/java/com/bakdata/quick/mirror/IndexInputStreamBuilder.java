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

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.IndexInputStream;
import com.bakdata.quick.mirror.range.extractor.SchemaExtractor;
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
 * Contains the logic of consuming the input topic and repartitioning of the topic if a rangeKey field is given.
 * Otherwise, the consumed input stream from the topic is returned.
 */
@Singleton
public class IndexInputStreamBuilder {
    private final SchemaExtractor schemaExtractor;
    private final ConversionProvider conversionProvider;

    @Inject
    public IndexInputStreamBuilder(final SchemaExtractor schemaExtractor, final ConversionProvider conversionProvider) {
        this.schemaExtractor = schemaExtractor;
        this.conversionProvider = conversionProvider;
    }

    /**
     * Consumes the input topic and does a repartitioning based on the range key.
     */
    public <K, R, V> IndexTopologyData<R, V> consume(final QuickTopologyData<K, V> topologyData,
        final StreamsBuilder streamsBuilder, @Nullable final String rangeKey) {
        final QuickTopicData<K, V> topicData = topologyData.getTopicData();
        final QuickData<K> keyData = topicData.getKeyData();
        final Serde<K> keySerde = keyData.getSerde();
        final QuickData<V> valueData = topicData.getValueData();
        final Serde<V> valueSerde = valueData.getSerde();
        final KStream<K, V> inputStream =
            streamsBuilder.stream(topologyData.getInputTopics().get(0), Consumed.with(keySerde, valueSerde));
        if (rangeKey != null) {
            return this.repartitionStreamOnRangeKey(inputStream, valueData, rangeKey);
        }
        return getTopologyDataFromInputStream(inputStream, valueData, keyData);
    }

    // The cast is safe. The generic types of R and K are equal when the rangeKey is null
    @SuppressWarnings("unchecked")
    private static <K, R, V> IndexTopologyData<R, V> getTopologyDataFromInputStream(
        final KStream<K, V> inputStream, final QuickData<V> valueData, final QuickData<K> keyData) {
        final IndexInputStream<K, V> indexInputStream = new IndexInputStream<>(keyData, valueData);
        return (IndexTopologyData<R, V>) new IndexTopologyData<>(indexInputStream, inputStream);
    }

    private <K, R, V> IndexTopologyData<R, V> repartitionStreamOnRangeKey(final KStream<K, V> inputStream,
        final QuickData<V> valueData, final String rangeKey) {
        final ParsedSchema parsedSchema = Objects.requireNonNull(valueData.getParsedSchema());
        final QuickTopicType quickTopicType =
            this.schemaExtractor.getFieldTypeExtractor().extract(parsedSchema, rangeKey);
        final QuickData<R> repartitionedKeyData = this.getRepartitionedKeyData(quickTopicType);
        final IndexInputStream<R, V> indexInputStream = new IndexInputStream<>(repartitionedKeyData, valueData);
        final Serde<R> repartitionedKeySerde = repartitionedKeyData.getSerde();
        final Serde<V> valueSerde = valueData.getSerde();
        final KStream<R, V> repartitionedStream = inputStream.selectKey(
                (key, value) -> this.<R, V>getRangeKeyValue(Objects.requireNonNull(rangeKey), value, quickTopicType))
            .repartition(Repartitioned.with(repartitionedKeySerde, valueSerde));
        return new IndexTopologyData<>(indexInputStream, repartitionedStream);
    }

    /**
     * Gets the repartitioned key data.
     */
    private <R> QuickData<R> getRepartitionedKeyData(final QuickTopicType quickTopicType) {
        final Serde<R> repartitionedKeySerde = this.conversionProvider.getSerde(quickTopicType, true);
        final TypeResolver<R> typeResolver = this.conversionProvider.getTypeResolver(quickTopicType, null);
        return new QuickData<>(quickTopicType, repartitionedKeySerde, typeResolver, null);
    }

    /**
     * Extracts the value of a given field.
     */
    private <R, V> R getRangeKeyValue(final String fieldName, final V value, final QuickTopicType quickTopicType) {
        if (value != null) {
            final Class<R> classType = this.conversionProvider.getClassType(quickTopicType);
            final FieldValueExtractor<V> fieldValueExtractor = this.schemaExtractor.getFieldValueExtractor();
            return fieldValueExtractor.extract(value, fieldName, classType);
        }
        throw new MirrorTopologyException("The value should not be null. Check you input topic data.");
    }

    /**
     * Contains a tuple of the {@link IndexInputStream} and the {@link KStream}.
     */
    @Value
    public static class IndexTopologyData<K, V> {
        IndexInputStream<K, V> indexInputStream;
        KStream<K, V> stream;
    }
}
