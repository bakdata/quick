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
import com.bakdata.quick.mirror.range.extractor.AvroExtractor;
import com.bakdata.quick.mirror.range.extractor.ProtoExtractor;
import com.bakdata.quick.mirror.range.extractor.RangeFieldValueExtractor;
import com.bakdata.quick.mirror.range.padder.IntPadder;
import com.bakdata.quick.mirror.range.padder.LongPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.jetbrains.annotations.NotNull;

/**
 * Creates range indexes for a Mirror's state store
 */
@Slf4j
public class RangeIndexer<K, V> {
    @NonNull
    private final ZeroPadder keyZeroPadder;
    @NonNull
    private final ZeroPadder valueZeroPadder;
    @Nullable
    private RangeFieldValueExtractor rangeFieldValueExtractor;

    private final String rangeField;

    /**
     * Creates the zero padder for the key and sets the range field value extractor based on the schema type.
     * It then reads the range field type form the schema and sets the value zero padder for the range field.
     */
    public RangeIndexer(final QuickTopicType keyType, final QuickTopicType valueType,
        final ParsedSchema parsedSchema, final String rangeField) {
        this.rangeField = rangeField;
        this.keyZeroPadder = createKeyZeroPadder(keyType);
        this.valueZeroPadder = this.createValueZeroPadder(valueType, parsedSchema);
    }

    /**
     * Creates the range index for a given key over a specific range field First the value is converted to Avro generic
     * record or Protobuf message. Then the value is extracted from the schema. Depending on the type (integer or long)
     * of the key and value zero paddings are appended to the left side of the key and value, and they are contaminated
     * with an <b>_</b>.
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
     * <p>
     * And the <i>range field</i> is <i>timestamp</i> with the value of 5. The returned value would be
     * 0000000001_0000000005
     */
    public String createIndex(final K key, final V value) {
        if (this.rangeFieldValueExtractor == null) {
            throw new MirrorTopologyException("Supported values are Avro and Protobuf");
        }
        final Object rangeFieldValue = this.rangeFieldValueExtractor.extractValue(value, this.rangeField);

        return String.format("%s_%s", this.keyZeroPadder.padZero(key), this.valueZeroPadder.padZero(rangeFieldValue));
    }

    @NonNull
    private static ZeroPadder<? extends Number> createKeyZeroPadder(final QuickTopicType topicType) {
        if (topicType == QuickTopicType.INTEGER) {
            log.trace("Creating integer zero padder for key");
            return new IntPadder();
        } else if (topicType == QuickTopicType.LONG) {
            log.trace("Creating long zero padder for key");
            return new LongPadder();
        }
        throw new MirrorTopologyException("Key value should be either integer or mirror");
    }

    @NonNull
    private ZeroPadder<? extends Number> createValueZeroPadder(final QuickTopicType topicType,
        final ParsedSchema parsedSchema) {
        if (topicType == QuickTopicType.AVRO) {
            this.rangeFieldValueExtractor = new AvroExtractor();
            return this.getZeroPadderForAvroSchema((Schema) parsedSchema.rawSchema());
        } else if (topicType == QuickTopicType.PROTOBUF) {
            this.rangeFieldValueExtractor = new ProtoExtractor();
            return this.getZeroPadderForProtobufSchema((ProtobufSchema) parsedSchema);
        }
        throw new MirrorTopologyException("Supported values are Avro and Protobuf");
    }

    @NotNull
    private ZeroPadder<? extends Number> getZeroPadderForAvroSchema(final Schema avroSchema) {
        final Type fieldType = avroSchema.getField(this.rangeField).schema().getType();
        if (fieldType == Type.INT) {
            log.trace("Creating integer zero padder for avro value");
            return new IntPadder();
        } else if (fieldType == Type.LONG) {
            log.trace("Creating long zero padder for avro value");
            return new LongPadder();
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }

    @NotNull
    private ZeroPadder<? extends Number> getZeroPadderForProtobufSchema(final ProtobufSchema parsedSchema) {
        final Descriptors.Descriptor descriptor = parsedSchema.toDescriptor();
        final JavaType fieldType = descriptor.findFieldByName(this.rangeField).getJavaType();
        if (fieldType == JavaType.INT) {
            log.trace("Creating integer zero padder for protobuf value");
            return new IntPadder();
        } else if (fieldType == JavaType.LONG) {
            log.trace("Creating long zero padder for protobuf value");
            return new LongPadder();
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }
}
