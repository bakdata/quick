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
import com.bakdata.quick.mirror.range.extractor.type.AvroTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.ProtoTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.GenericRecordValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Creates range indexes for a Mirror's state store.
 */
@Slf4j
public final class DefaultRangeIndexer<K, V> implements RangeIndexer<K, V> {
    private final FieldTypeExtractor fieldTypeExtractor;
    private final ParsedSchema parsedSchema;
    private final String rangeField;

    private DefaultRangeIndexer(final FieldTypeExtractor fieldTypeExtractor, final ParsedSchema parsedSchema,
        final String rangeField) {
        this.fieldTypeExtractor = fieldTypeExtractor;
        this.parsedSchema = parsedSchema;
        this.rangeField = rangeField;
    }

    public static <K, V> DefaultRangeIndexer<K, V> create(final ParsedSchema parsedSchema,
        final String rangeField) {
        log.debug("Type Avro detected");
        if (parsedSchema.schemaType().equals(AvroSchema.TYPE)) {
            return new DefaultRangeIndexer<>(new AvroTypeExtractor(), parsedSchema, rangeField);
        } else if (parsedSchema.schemaType().equals(ProtobufSchema.TYPE)) {
            return new DefaultRangeIndexer<>(new ProtoTypeExtractor(), parsedSchema, rangeField);
        }
        throw new MirrorTopologyException("Not supported schema type. Supported ones: Avro, Protobuf");
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
    public <F> String createIndex(final K key, final V value) {
        final ZeroPadder<F> zeroPadder = this.fieldTypeExtractor.extractType(this.parsedSchema, this.rangeField);
        if(value instanceof GenericRecord) {
            final FieldValueExtractor<GenericRecord, F> rangeFieldValue = new GenericRecordValueExtractor<>(zeroPadder);
            final String paddedValue = zeroPadder.padZero(rangeFieldValue.extractValue((GenericRecord) value, this.rangeField));
            return this.createRangeIndexFormat(key, paddedValue);
        } else if (value instanceof Message) {
            final FieldValueExtractor<Message, F> rangeFieldValue = new MessageValueExtractor<>(zeroPadder);
            final String paddedValue = zeroPadder.padZero(rangeFieldValue.extractValue((Message) value, this.rangeField));
            return this.createRangeIndexFormat(key, paddedValue);
        }
        throw new MirrorTopologyException("Not supported type.");
    }
}
