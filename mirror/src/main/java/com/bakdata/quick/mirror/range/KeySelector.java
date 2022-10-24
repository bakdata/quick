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
import java.util.Map;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serde;

public final class KeySelector<V> {
    @Getter
    private final QuickTopicType keyType;
    private final FieldValueExtractor<? super V> fieldValueExtractor;

    private KeySelector(final QuickTopicType keyType, final FieldValueExtractor<? super V> fieldValueExtractor) {
        this.keyType = keyType;
        this.fieldValueExtractor = fieldValueExtractor;
    }

    @SuppressWarnings("unchecked")
    public static <V> KeySelector<V> create(final ParsedSchema parsedSchema, final String rangeKey) {
        if (AvroSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldTypeExtractor avroTypeExtractor = new AvroTypeExtractor();
            final QuickTopicType keyType = avroTypeExtractor.extract(parsedSchema, rangeKey);
            final FieldValueExtractor<V> fieldValueExtractor =
                (FieldValueExtractor<V>) new GenericRecordValueExtractor();
            return new KeySelector<>(keyType, fieldValueExtractor);

        } else if (ProtobufSchema.TYPE.equals(parsedSchema.schemaType())) {
            final FieldTypeExtractor protoTypeExtractor = new ProtoTypeExtractor();
            final QuickTopicType keyType = protoTypeExtractor.extract(parsedSchema, rangeKey);
            final FieldValueExtractor<V> fieldValueExtractor = (FieldValueExtractor<V>) new MessageValueExtractor();
            return new KeySelector<>(keyType, fieldValueExtractor);
        }
        throw new MirrorTopologyException("Unsupported schema type.");
    }

    public <T> T getRangeKey(final String fieldName, final V value) {
        if (value != null) {
            return this.fieldValueExtractor.extract(value, fieldName, this.keyType.getClassType());
        }
        throw new MirrorTopologyException("The value should not be null. Check you input topic data.");
    }

    public <T> Serde<T> getKeySerde(){
        // TODO: Get this from KafkaConfig
        final Map<String, String> configs = Map.of("schema.registry.url", "1.2.3.4");
        return this.keyType.getSerde(configs, true);
    }
}
