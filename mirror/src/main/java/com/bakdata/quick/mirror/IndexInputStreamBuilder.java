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
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Repartitioned;

/**
 * Contains the logic of consuming the input topic and repartitioning of the topic if a rangeKey field is given.
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
     * Consumes the input topic into a {@link IndexInputStream}. There are two possible scenarios for consuming the
     * input stream.
     * <ul>
     *     <li><b>rangeKey is null</b>: The records are consumed from the input topic into a
     *     {@link IndexInputStream}. We
     *     cast K to R because these generic types are equal.</li>
     *     <li><b>rangeKey is not null</b>: A repartitioning takes place. First the rangeKey field type is extracted
     *     from
     *     the {@link ParsedSchema}. Then other other information of the field like the SerDe, TypeResolver are set.
     *     After that, a {@link KStream#selectKey(KeyValueMapper, Named)} operation is executed to set the new key of
     *     the records. Along with this operation a {@link KStream#repartition()} takes place to repartition the
     *     topic base on the <i>rangeKey</i>. In the end, the method returns a {@link IndexInputStream} with the
     *     repartitioned stream and data for the key and value.</li>
     * </ul>
     *
     * @param topologyData Contains the information for the Kafka Streams topology
     * @param streamsBuilder Specifies the Kafka streams topology
     * @param rangeKey A nullable field determining if the key of the topic should change or not
     * @param cleanUp Defines if the mirror is running a cleanup job or not
     * @param <K> Type of the topic key
     * @param <V> Type of the topic value
     * @param <R> Type of the rangeKey field
     * @return {@link IndexInputStream} Containing the key, value data and the KStream
     */
    public <K, R, V> IndexInputStream<R, V> consume(final QuickTopologyData<K, V> topologyData,
        final StreamsBuilder streamsBuilder, @Nullable final String rangeKey, final boolean cleanUp) {
        final QuickTopicData<K, V> topicData = topologyData.getTopicData();
        final QuickData<K> keyData = topicData.getKeyData();
        final Serde<K> keySerde = keyData.getSerde();
        final QuickData<V> valueData = topicData.getValueData();
        final Serde<V> valueSerde = valueData.getSerde();
        final KStream<K, V> inputStream =
            streamsBuilder.stream(topologyData.getInputTopics().get(0), Consumed.with(keySerde, valueSerde));
        if (rangeKey != null) {
            return this.repartitionOnRangeKey(inputStream, valueData, rangeKey);
        }
        return getIndexInputStream(inputStream, valueData, keyData);
    }

    // The cast is safe. The generic types of R and K are equal when the rangeKey is null
    @SuppressWarnings("unchecked")
    private static <K, R, V> IndexInputStream<R, V> getIndexInputStream(
        final KStream<K, V> inputStream, final QuickData<V> valueData, final QuickData<K> keyData) {
        return (IndexInputStream<R, V>) new IndexInputStream<>(keyData, valueData, inputStream);
    }

    private <K, R, V> IndexInputStream<R, V> repartitionOnRangeKey(final KStream<K, V> inputStream,
        final QuickData<V> valueData, final String rangeKey) {
        final ParsedSchema parsedSchema = Objects.requireNonNull(valueData.getParsedSchema());
        final QuickTopicType quickTopicType =
            this.schemaExtractor.getFieldTypeExtractor().extract(parsedSchema, rangeKey);
        final QuickData<R> repartitionedKeyData = this.createRepartitionedKeyData(quickTopicType);

        final KStream<R, V> repartitionedStream =
            this.getRepartitionedStream(inputStream, valueData, rangeKey, quickTopicType, repartitionedKeyData);

        return new IndexInputStream<>(repartitionedKeyData, valueData, repartitionedStream);
    }

    /**
     * Creates the repartitioned key data.
     */
    private <R> QuickData<R> createRepartitionedKeyData(final QuickTopicType quickTopicType) {
        final Serde<R> repartitionedKeySerde = this.conversionProvider.getSerde(quickTopicType, true);
        final TypeResolver<R> typeResolver = this.conversionProvider.getTypeResolver(quickTopicType, null);
        return new QuickData<>(quickTopicType, repartitionedKeySerde, typeResolver, null);
    }

    /**
     * Performs a selectKey and repartition on the rangeKey field.
     */
    private <K, R, V> KStream<R, V> getRepartitionedStream(final KStream<K, V> inputStream,
        final QuickData<V> valueData, final String rangeKey,
        final QuickTopicType quickTopicType, final QuickData<R> repartitionedKeyData) {
        final Serde<R> rangeKeySerde = repartitionedKeyData.getSerde();
        final Serde<V> valueSerde = valueData.getSerde();
        return inputStream.selectKey((key, value) -> this.<R, V>getRangeKeyValue(rangeKey, value, quickTopicType),
                Named.as("rangeKeySelector"))
            .repartition(Repartitioned.with(rangeKeySerde, valueSerde));
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
}
