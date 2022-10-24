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

package com.bakdata.quick.mirror.range.extractor.type;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.common.type.QuickTopicType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

/**
 * Implements the logic of extracting the {@link QuickTopicType} from a field in an Avro schema.
 */
@Slf4j
public class AvroTypeExtractor implements FieldTypeExtractor {
    @Override
    public QuickTopicType extract(final ParsedSchema parsedSchema, final String fieldName) {
        final Schema avroSchema = (Schema) parsedSchema.rawSchema();
        final Schema.Type fieldType = getAvroFieldType(avroSchema, fieldName);
        log.debug("Field Type is {}", fieldType);

        if (fieldType == Schema.Type.INT) {
            return QuickTopicType.INTEGER;
        } else if (fieldType == Schema.Type.LONG) {
            return QuickTopicType.LONG;
        } else if (fieldType == Schema.Type.STRING) {
            return QuickTopicType.STRING;
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }

    private static Schema.Type getAvroFieldType(final Schema avroSchema, final String fieldName) {
        final Field field = avroSchema.getField(fieldName);
        if (field == null) {
            final String errorMessage =
                String.format("The defined range field %s does not exist in your Avro schema.", fieldName);
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
