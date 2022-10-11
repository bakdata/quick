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
import com.bakdata.quick.mirror.range.padder.EndRange;
import com.bakdata.quick.mirror.range.padder.IntPadder;
import com.bakdata.quick.mirror.range.padder.LongPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.micronaut.core.util.StringUtils;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

/**
 * Creates range indexes for a Mirror's state store.
 */
@Slf4j
public final class DefaultRangeIndexer<K, V, F> implements RangeIndexer<K, V> {
    private final ZeroPadder<K> keyZeroPadder;
    private final ZeroPadder<F> valueZeroPadder;
    private final RangeFieldValueExtractor<V, F> rangeFieldValueExtractor;
    private final String rangeField;

    private DefaultRangeIndexer(final ZeroPadder<K> keyZeroPadder, final ZeroPadder<F> valueZeroPadder,
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
    public static <K, V, F> DefaultRangeIndexer<K, V, F> createRangeIndexer(
        final QuickTopicType keyType,
        final ParsedSchema parsedSchema,
        final String rangeField) {

        final ZeroPadder<K> keyZeroPadder = createZeroPadderForTopicType(keyType, EndRange.INCLUSIVE);

        switch (parsedSchema.schemaType()) {
            case (AvroSchema.TYPE):
                return getDefaultRangeIndexerForAvroSchema(parsedSchema, rangeField, keyZeroPadder);
            case (ProtobufSchema.TYPE):
                return getDefaultRangeIndexerForProtobuf(parsedSchema, rangeField, keyZeroPadder);
            default:
                throw new MirrorTopologyException("Unsupported schema type.");
        }
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
     * And the <i>range field</i> is <i>timestamp</i> with the value of 5. The returned value would be
     * 0000000001_0000000005
     */
    @Override
    public String createIndex(final K key, final V value) {
        final F rangeFieldValue = this.rangeFieldValueExtractor.extractValue(value, this.rangeField);
        return String.format("%s_%s", this.keyZeroPadder.padZero(key), this.valueZeroPadder.padZero(rangeFieldValue));
    }

    /**
     * Creates the range index for a given key and a value string type.
     */
    public String createIndex(final K key, final String value) {
        if (!StringUtils.isDigits(value)) {
            throw new MirrorTopologyException("The string value should be a series of digits");
        }

        final F number = this.valueZeroPadder.getEndOfRange(value);
        final String paddedValue = this.valueZeroPadder.padZero(number);

        final String paddedKey = this.keyZeroPadder.padZero(key);

        return String.format("%s_%s", paddedKey, paddedValue);
    }

    private static <K, V, F> DefaultRangeIndexer<K, V, F> getDefaultRangeIndexerForAvroSchema(
        final ParsedSchema parsedSchema,
        final String rangeField, final ZeroPadder<K> keyZeroPadder) {
        log.debug("Type Avro detected");
        final QuickTopicType rangeValueType = getValueTypeForAvroSchema((Schema) parsedSchema.rawSchema(), rangeField);
        final ZeroPadder<F> valueZeroPadder = createZeroPadderForTopicType(rangeValueType, EndRange.EXCLUSIVE);
        final RangeFieldValueExtractor avroExtractor = new AvroValueExtractor<>(valueZeroPadder.getPadderClass());
        return new DefaultRangeIndexer<>(keyZeroPadder, valueZeroPadder, avroExtractor, rangeField);
    }

    private static <K, V, F> DefaultRangeIndexer<K, V, F> getDefaultRangeIndexerForProtobuf(
        final ParsedSchema parsedSchema,
        final String rangeField, final ZeroPadder<K> keyZeroPadder) {
        log.debug("Type Protobuf detected");
        final QuickTopicType rangeValueType =
            getValueTypeForProtobufSchema((ProtobufSchema) parsedSchema, rangeField);
        final ZeroPadder<F> valueZeroPadder = createZeroPadderForTopicType(rangeValueType, EndRange.EXCLUSIVE);
        final RangeFieldValueExtractor protoExtractor = new ProtoValueExtractor<>(valueZeroPadder.getPadderClass());
        return new DefaultRangeIndexer<>(keyZeroPadder, valueZeroPadder, protoExtractor, rangeField);
    }

    @SuppressWarnings("unchecked")
    private static <K> ZeroPadder<K> createZeroPadderForTopicType(final QuickTopicType topicType,
        final EndRange endRange) {
        if (topicType == QuickTopicType.INTEGER) {
            log.trace("Creating integer zero padder for key");
            return (ZeroPadder<K>) new IntPadder(endRange);
        } else if (topicType == QuickTopicType.LONG) {
            log.trace("Creating long zero padder for key");
            return (ZeroPadder<K>) new LongPadder(endRange);
        }
        throw new MirrorTopologyException("Key value should be either integer or long");
    }

    private static QuickTopicType getValueTypeForAvroSchema(final Schema avroSchema,
        final String rangeField) {
        final Schema.Type fieldType = getAvroFieldType(avroSchema, rangeField);
        log.debug("Field Type is {}", fieldType);

        if (fieldType == Schema.Type.INT) {
            log.trace("Creating integer zero padder for avro value");
            return QuickTopicType.INTEGER;
        } else if (fieldType == Schema.Type.LONG) {
            log.trace("Creating long zero padder for avro value");
            return QuickTopicType.LONG;
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }

    private static QuickTopicType getValueTypeForProtobufSchema(final ProtobufSchema parsedSchema,
        final String rangeField) {
        final Descriptors.Descriptor descriptor = parsedSchema.toDescriptor();
        final FieldDescriptor field = descriptor.findFieldByName(rangeField);
        if (field == null) {
            final String errorMessage =
                String.format("The defined range field %s does not exist in your Proto schema.", rangeField);
            throw new MirrorTopologyException(errorMessage);
        }
        final JavaType fieldType = field.getJavaType();
        if (fieldType == JavaType.INT) {
            log.trace("Creating integer zero padder for protobuf value");
            return QuickTopicType.INTEGER;
        } else if (fieldType == JavaType.LONG) {
            log.trace("Creating long zero padder for protobuf value");
            return QuickTopicType.LONG;
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }

    private static Schema.Type getAvroFieldType(final Schema avroSchema, final String rangeField) {
        final Field field = avroSchema.getField(rangeField);
        if (field == null) {
            final String errorMessage =
                String.format("The defined range field %s does not exist in your Avro schema.", rangeField);
            throw new MirrorTopologyException(errorMessage);
        }
        final Schema fieldSchema = field.schema();
        if (fieldSchema.getType() == Schema.Type.UNION) {
            final List<Schema> fieldTypes = fieldSchema.getTypes();
            final Optional<Schema.Type> intLongSchemaType = fieldTypes.stream()
                .map(Schema::getType)
                .filter(schemaType -> schemaType == Schema.Type.INT || schemaType == Schema.Type.LONG)
                .findFirst();
            return intLongSchemaType.orElseThrow(
                () -> new MirrorTopologyException("The schema field should be int or long"));
        } else {
            return fieldSchema.getType();
        }
    }
}
