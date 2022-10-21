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
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadderFactory;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.micronaut.core.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * An indexer for string values. This indexer is used to build the default range index pattern when querying for
 * ranges.
 */
@Slf4j
public final class StringValueRangeIndexer<K> implements RangeIndexer<K, String> {
    private final FieldTypeExtractor fieldTypeExtractor;
    private final ParsedSchema parsedSchema;
    private final String rangeField;

    private StringValueRangeIndexer(final FieldTypeExtractor fieldTypeExtractor, final ParsedSchema parsedSchema,
        final String rangeField) {
        this.fieldTypeExtractor = fieldTypeExtractor;
        this.parsedSchema = parsedSchema;
        this.rangeField = rangeField;
    }

    /**
     * Creates the range index for a given key and a value string type.
     */
    public static <K> StringValueRangeIndexer<K> create(final ParsedSchema parsedSchema,
        final String rangeField) {
        log.debug("Type Avro detected");
        if (parsedSchema.schemaType().equals(AvroSchema.TYPE)) {
            return new StringValueRangeIndexer<>(new AvroTypeExtractor(), parsedSchema, rangeField);
        } else if (parsedSchema.schemaType().equals(ProtobufSchema.TYPE)) {
            return new StringValueRangeIndexer<>(new ProtoTypeExtractor(), parsedSchema, rangeField);
        }
        throw new MirrorTopologyException("Not supported");
    }

    @Override
    public <F> String createIndex(final K key, final String value) {
        if (!StringUtils.isDigits(value)) {
            throw new MirrorTopologyException("The string value should be a series of digits");
        }
        final QuickTopicType topicType = this.fieldTypeExtractor.extractType(this.parsedSchema, this.rangeField);
        final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
        final F number = zeroPadder.getEndOfRange(value);
        final String paddedValue = zeroPadder.padZero(number);

        return this.createRangeIndexFormat(key, paddedValue);
    }
}
