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
import com.bakdata.quick.mirror.range.extractor.type.AvroTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.ProtoTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.GenericRecordValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;

public final class KeySelector<K, V> {
    private final FieldTypeExtractor fieldTypeExtractor;
    private final FieldValueExtractor<V> fieldValueExtractor;

    private KeySelector(final FieldTypeExtractor fieldTypeExtractor, final FieldValueExtractor<V> fieldValueExtractor) {
        this.fieldTypeExtractor = fieldTypeExtractor;
        this.fieldValueExtractor = fieldValueExtractor;
    }

    public K getKey(final String rangeKey, final ParsedSchema parsedSchema, final V value) {
        if (value != null) {
            final QuickTopicType topicType = this.fieldTypeExtractor.extractType(parsedSchema, rangeKey);
            return this.fieldValueExtractor.extractValue(value, rangeKey, topicType.getClassType());
        }
        throw new MirrorTopologyException("Something went wrong!");
    }

    @SuppressWarnings("unchecked")
    public static <K, V> KeySelector<K, V> create(final ParsedSchema parsedSchema) {
        if (AvroSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldTypeExtractor avroTypeExtractor = new AvroTypeExtractor();
            final FieldValueExtractor<V> fieldValueExtractor = (FieldValueExtractor<V>) new GenericRecordValueExtractor();
            return new KeySelector<>(avroTypeExtractor, fieldValueExtractor);
        } else if (ProtobufSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldTypeExtractor avroTypeExtractor = new ProtoTypeExtractor();
            final FieldValueExtractor<V> fieldValueExtractor = (FieldValueExtractor<V>) new MessageValueExtractor();
            return new KeySelector<>(avroTypeExtractor, fieldValueExtractor);
        }
        throw new MirrorTopologyException("Unsupported schema type.");
    }
}
