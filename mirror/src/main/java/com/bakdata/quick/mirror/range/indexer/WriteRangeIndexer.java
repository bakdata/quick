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

import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadderFactory;
import io.confluent.kafka.schemaregistry.ParsedSchema;

/**
 * Implements the logic for range index for incoming data.
 */
public final class WriteRangeIndexer<K, V, F> implements RangeIndexer<K, V> {
    private final FieldValueExtractor<? super V> fieldValueExtractor;
    private final ZeroPadder<F> zeroPadder;
    private final String rangeField;

    private WriteRangeIndexer(final ZeroPadder<F> zeroPadder,
        final FieldValueExtractor<? super V> fieldValueExtractor,
        final String rangeField) {
        this.zeroPadder = zeroPadder;
        this.fieldValueExtractor = fieldValueExtractor;
        this.rangeField = rangeField;
    }

    /**
     * Creates the zero padder for the key and sets the range field value extractor based on the schema type.
     */
    public static <K, V, F> WriteRangeIndexer<K, V, F> create(final FieldTypeExtractor fieldTypeExtractor,
        final FieldValueExtractor<? super V> fieldValueExtractor,
        final ParsedSchema parsedSchema,
        final String rangeField) {
        final QuickTopicType topicType = fieldTypeExtractor.extract(parsedSchema, rangeField);
        final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
        return new WriteRangeIndexer<>(zeroPadder, fieldValueExtractor, rangeField);
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
            this.fieldValueExtractor.extract(value, this.rangeField, this.zeroPadder.getPadderClass());
        final String paddedValue = this.zeroPadder.padZero(number);
        return this.createRangeIndexFormat(key, paddedValue);
    }
}
