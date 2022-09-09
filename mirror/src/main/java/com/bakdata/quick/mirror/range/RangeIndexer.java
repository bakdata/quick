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

package com.bakdata.quick.mirror.range;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.extractor.AvroValueExtractor;
import com.bakdata.quick.mirror.range.extractor.ProtoValueExtractor;
import com.bakdata.quick.mirror.range.extractor.RangeFieldValueExtractor;
import com.bakdata.quick.mirror.range.padder.IntPadder;
import com.bakdata.quick.mirror.range.padder.LongPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;

/**
 * Creates range indexes for a Mirror's state store.
 */
@Slf4j
public final class RangeIndexer<K, V, F extends Number> {
    private final ZeroPadder<K> keyZeroPadder;
    private final ZeroPadder<F> valueZeroPadder;
    private final RangeFieldValueExtractor<V, F> rangeFieldValueExtractor;

    private final String rangeField;

    private RangeIndexer(final ZeroPadder<K> keyZeroPadder, final ZeroPadder<F> valueZeroPadder,
        final RangeFieldValueExtractor<V, F> rangeFieldValueExtractor, final String rangeField) {
        this.keyZeroPadder = keyZeroPadder;
        this.valueZeroPadder = valueZeroPadder;
        this.rangeFieldValueExtractor = rangeFieldValueExtractor;
        this.rangeField = rangeField;
    }

    /**
     * Creates the zero padder for the key and sets the range field value extractor based on the schema type. It then
     * reads the range field type form the schema and sets the value zero padder for the range field.
     */
    public static <K, V, F extends Number> RangeIndexer<K, V, F> createRangeIndexer(
        final QuickTopicType keyType,
        final QuickTopicType valueType,
        final ParsedSchema parsedSchema,
        final String rangeField) {

        final ZeroPadder<K> keyZeroPadder = createKeyZeroPadder(keyType);
        final ZeroPadder<F> valueZeroPadder = createValueZeroPadder(valueType, parsedSchema, rangeField);
        if (valueType == QuickTopicType.AVRO) {
            final RangeFieldValueExtractor avroExtractor = new AvroValueExtractor<>(valueZeroPadder.getPadderClass());
            return new RangeIndexer<>(keyZeroPadder, valueZeroPadder, avroExtractor, rangeField);
        } else if (valueType == QuickTopicType.PROTOBUF) {
            final RangeFieldValueExtractor protoExtractor = new ProtoValueExtractor<>(valueZeroPadder.getPadderClass());
            return new RangeIndexer<>(keyZeroPadder, valueZeroPadder, protoExtractor, rangeField);
        }
        throw new MirrorTopologyException("Key value should be either integer or mirror");
    }

    /**
     * Creates the range index for a given key over a specific range field First the value is converted to Avro generic
     * record or Protobuf message. Then the value is extracted from the schema. Depending on the type (integer or long)
     * of the key and value zero paddings are appended to the left side of the key and value, and they are contaminated
     * with an <b>_</b>.
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
     * And the <i>range field</i> is <i>timestamp</i> with the value of 5. The returned value would be
     * 0000000001_0000000005
     */
    public String createIndex(final K key, final V value) {
        final F rangeFieldValue = this.rangeFieldValueExtractor.extractValue(value, this.rangeField);
        return String.format("%s_%s", this.keyZeroPadder.padZero(key), this.valueZeroPadder.padZero(rangeFieldValue));
    }

    public String createIndex(final K key, final String from) {
        final Class<F> valuePadderClass = this.valueZeroPadder.getPadderClass();
        final String paddedValue;
        final F field = valuePadderClass.cast(from);
        paddedValue = this.valueZeroPadder.padZero(field);
        final String paddedKey = this.keyZeroPadder.padZero(key);

        return String.format("%s_%s", paddedKey, paddedValue);
    }

    private static <K> ZeroPadder<K> createKeyZeroPadder(final QuickTopicType topicType) {
        if (topicType == QuickTopicType.INTEGER) {
            log.trace("Creating integer zero padder for key");
            return (ZeroPadder<K>) new IntPadder();
        } else if (topicType == QuickTopicType.LONG) {
            log.trace("Creating long zero padder for key");
            return (ZeroPadder<K>) new LongPadder();
        }
        throw new MirrorTopologyException("Key value should be either integer or mirror");
    }

    private static <F extends Number> ZeroPadder<F> createValueZeroPadder(final QuickTopicType topicType,
        final ParsedSchema parsedSchema, final String rangeField) {
        if (topicType == QuickTopicType.AVRO) {
            return getZeroPadderForAvroSchema((Schema) parsedSchema.rawSchema(), rangeField);
        } else if (topicType == QuickTopicType.PROTOBUF) {
            return getZeroPadderForProtobufSchema((ProtobufSchema) parsedSchema, rangeField);
        }
        throw new MirrorTopologyException("Supported values are Avro and Protobuf");
    }

    private static <F extends Number> ZeroPadder<F> getZeroPadderForAvroSchema(
        final Schema avroSchema,
        final String rangeField) {
        final Schema.Type fieldType = avroSchema.getField(rangeField).schema().getType();
        if (fieldType == Schema.Type.INT) {
            log.trace("Creating integer zero padder for avro value");
            return (ZeroPadder<F>) new IntPadder();
        } else if (fieldType == Schema.Type.LONG) {
            log.trace("Creating long zero padder for avro value");
            return (ZeroPadder<F>) new LongPadder();
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }

    private static <V, F extends Number> ZeroPadder<F> getZeroPadderForProtobufSchema(
        final ProtobufSchema parsedSchema,
        final String rangeField) {
        final Descriptors.Descriptor descriptor = parsedSchema.toDescriptor();
        final JavaType fieldType = descriptor.findFieldByName(rangeField).getJavaType();
        if (fieldType == JavaType.INT) {
            log.trace("Creating integer zero padder for protobuf value");
            return (ZeroPadder<F>) new IntPadder();
        } else if (fieldType == JavaType.LONG) {
            log.trace("Creating long zero padder for protobuf value");
            return (ZeroPadder<F>) new LongPadder();
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }
}
