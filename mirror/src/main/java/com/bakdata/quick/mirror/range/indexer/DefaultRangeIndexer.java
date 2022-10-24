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

package com.bakdata.quick.mirror.range.indexer;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.extractor.type.AvroTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.ProtoTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.GenericRecordValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadderFactory;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;

/**
 * Creates range indexes for a Mirror's state store.
 */
public final class DefaultRangeIndexer<K, V, F> implements RangeIndexer<K, V> {
    private final ZeroPadder<F> zeroPadder;
    private final FieldValueExtractor<? super V> fieldValueExtractor;
    private final String rangeField;

    private DefaultRangeIndexer(final ZeroPadder<F> zeroPadder,
        final FieldValueExtractor<? super V> fieldValueExtractor,
        final String rangeField) {
        this.zeroPadder = zeroPadder;
        this.fieldValueExtractor = fieldValueExtractor;
        this.rangeField = rangeField;
    }

    /**
     * Creates the zero padder for the key and sets the range field value extractor based on the schema type. It then
     * reads the range field type form the schema and sets the value zero padder for the range field.
     */
    @SuppressWarnings("unchecked")
    public static <K, V, F> DefaultRangeIndexer<K, V, F> create(final ParsedSchema parsedSchema,
        final String rangeField) {
        if (AvroSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldValueExtractor<V> fieldValueExtractor =
                (FieldValueExtractor<V>) new GenericRecordValueExtractor();
            final FieldTypeExtractor fieldTypeExtractor = new AvroTypeExtractor();
            final QuickTopicType topicType = fieldTypeExtractor.extractType(parsedSchema, rangeField);
            final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
            return new DefaultRangeIndexer<>(zeroPadder, fieldValueExtractor,
                rangeField);
        } else if (ProtobufSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldValueExtractor<V> messageValueExtractor =
                (FieldValueExtractor<V>) new MessageValueExtractor();
            final FieldTypeExtractor fieldTypeExtractor = new ProtoTypeExtractor();
            final QuickTopicType topicType = fieldTypeExtractor.extractType(parsedSchema, rangeField);
            final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
            return new DefaultRangeIndexer<>(zeroPadder, messageValueExtractor,
                rangeField);
        }
        throw new MirrorTopologyException("Unsupported schema type.");

    }

    /**
     * Creates the range index for a given key over a specific range field.
     *
     * <p>
     * First the value is converted to Avro generic record or Protobuf message. Then the value is extracted from the
     * schema. Depending on the type (integer or long) of the key and value zero paddings are appended to the left side
     * of the key and value, and they are contaminated with an <b>_</b>.
     *
     * <p>
     * Imagine the incoming record has a key of type integer with the value 1. The value is a proto schema with the
     * following schema:
     *
     * <pre>{@code
     * message ProtoRangeQueryTest {
     *   int32 userId = 1;
     *   int32 timestamp = 2;
     * }
     *  }</pre>
     *
     * <p>
     * And the <i>range field</i> is <i>timestamp</i> with the value of 5. The returned value would be 1_0000000005
     */
    @Override
    public String createIndex(final K key, final V value) {
        final F number =
            this.fieldValueExtractor.extractValue(value, this.rangeField, this.zeroPadder.getPadderClass());
        final String paddedValue = this.zeroPadder.padZero(number);
        return this.createRangeIndexFormat(key, paddedValue);
    }
}
