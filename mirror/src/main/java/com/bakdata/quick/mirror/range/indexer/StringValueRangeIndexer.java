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
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadderFactory;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.micronaut.core.util.StringUtils;

/**
 * An indexer for string values. This indexer is used to build the default range index pattern when querying for
 * ranges.
 */
public final class StringValueRangeIndexer<K, F> implements RangeIndexer<K, String> {
    private final ZeroPadder<F> zeroPadder;

    private StringValueRangeIndexer(final ZeroPadder<F> zeroPadder) {
        this.zeroPadder = zeroPadder;
    }

    /**
     * Creates the range index for a given key and a value string type.
     */
    public static <K, F> StringValueRangeIndexer<K, F> create(final ParsedSchema parsedSchema,
        final String rangeField) {
        if (parsedSchema.schemaType().equals(AvroSchema.TYPE)) {
            final AvroTypeExtractor fieldTypeExtractor = new AvroTypeExtractor();
            final QuickTopicType topicType = fieldTypeExtractor.extractType(parsedSchema, rangeField);
            final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
            return new StringValueRangeIndexer<>(zeroPadder);
        } else if (parsedSchema.schemaType().equals(ProtobufSchema.TYPE)) {
            final AvroTypeExtractor fieldTypeExtractor = new AvroTypeExtractor();
            final QuickTopicType topicType = fieldTypeExtractor.extractType(parsedSchema, rangeField);
            final ZeroPadder<F> zeroPadder = ZeroPadderFactory.create(topicType);
            return new StringValueRangeIndexer<>(zeroPadder);
        }
        throw new MirrorTopologyException("Unsupported schema type.");
    }

    @Override
    public String createIndex(final K key, final String value) {
        if (!StringUtils.isDigits(value)) {
            throw new MirrorTopologyException("The string value should be a series of digits");
        }
        final F number = this.zeroPadder.getEndOfRange(value);
        final String paddedValue = this.zeroPadder.padZero(number);

        return this.createRangeIndexFormat(key, paddedValue);
    }
}
