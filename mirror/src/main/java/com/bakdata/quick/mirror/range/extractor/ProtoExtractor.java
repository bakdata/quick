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

package com.bakdata.quick.mirror.range.extractor;

import com.bakdata.quick.common.condition.ProtobufSchemaFormatCondition;
import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.ProtoTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
import com.google.protobuf.Message;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

/**
 * Creates the {@link ProtoTypeExtractor} and the {@link MessageValueExtractor}.
 */
@Requires(condition = ProtobufSchemaFormatCondition.class)
@Singleton
public class ProtoExtractor implements SchemaExtractor {
    private final FieldTypeExtractor fieldTypeExtractor;
    private final FieldValueExtractor<Message> fieldValueExtractor;

    /**
     * Default constructor.
     */
    public ProtoExtractor() {
        this.fieldTypeExtractor = new ProtoTypeExtractor();
        this.fieldValueExtractor = new MessageValueExtractor<>();
    }

    @Override
    public FieldTypeExtractor getFieldTypeExtractor() {
        return this.fieldTypeExtractor;
    }

    @Override
    public FieldValueExtractor<Message> getFieldValueExtractor() {
        return this.fieldValueExtractor;
    }
}
