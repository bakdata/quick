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
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import java.util.Map;

/**
 * Implements the logic of extracting the {@link QuickTopicType} from a field in a Protobuf schema.
 */
public class ProtoTypeExtractor implements FieldTypeExtractor {

    private static final Map<JavaType, QuickTopicType> typeMap = Map.of(
        JavaType.INT, QuickTopicType.INTEGER,
        JavaType.LONG, QuickTopicType.LONG);

    @Override
    public QuickTopicType extract(final ParsedSchema parsedSchema, final String fieldName) {
        final ProtobufSchema protobufSchema = (ProtobufSchema) parsedSchema;
        final Descriptors.Descriptor descriptor = protobufSchema.toDescriptor();
        final FieldDescriptor fieldDescriptor = descriptor.findFieldByName(fieldName);
        if (fieldDescriptor == null) {
            final String errorMessage =
                String.format("The defined range field %s does not exist in your Proto schema.", fieldName);
            throw new MirrorTopologyException(errorMessage);
        }
        final JavaType fieldType = fieldDescriptor.getJavaType();
        final QuickTopicType type = typeMap.getOrDefault(fieldType, null);
        if (type == null) {
            throw new MirrorTopologyException(String.format("Unsupported field type %s.", fieldType));
        }
        return type;
    }
}
