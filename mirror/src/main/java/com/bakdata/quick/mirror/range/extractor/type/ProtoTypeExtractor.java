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
import com.bakdata.quick.mirror.range.padder.EndRange;
import com.bakdata.quick.mirror.range.padder.IntPadder;
import com.bakdata.quick.mirror.range.padder.LongPadder;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoTypeExtractor implements FieldTypeExtractor {

    @Override
    @SuppressWarnings("unchecked")
    public <F> ZeroPadder<F> extractType(final ParsedSchema parsedSchema, final String field) {
        final ProtobufSchema protobufSchema = (ProtobufSchema) parsedSchema;
        final Descriptors.Descriptor descriptor = protobufSchema.toDescriptor();
        final FieldDescriptor fieldDescriptor = descriptor.findFieldByName(field);
        if (fieldDescriptor == null) {
            final String errorMessage =
                String.format("The defined range field %s does not exist in your Proto schema.", field);
            throw new MirrorTopologyException(errorMessage);
        }
        final JavaType fieldType = fieldDescriptor.getJavaType();
        if (fieldType == JavaType.INT) {
            log.trace("Creating integer zero padder for avro value");
            return (ZeroPadder<F>) new IntPadder(EndRange.EXCLUSIVE);
        } else if (fieldType == JavaType.LONG) {
            log.trace("Creating long zero padder for avro value");
            return (ZeroPadder<F>) new LongPadder(EndRange.EXCLUSIVE);
        }
        throw new MirrorTopologyException("Range field value should be either integer or long");
    }
}
