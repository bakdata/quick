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

package com.bakdata.quick.mirror.range.extractor.value;


import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the value extraction logic for a Protobuf message.
 */
@Slf4j
public class MessageValueExtractor implements FieldValueExtractor<Message> {

    /**
     * Extracts the value from a Protobuf message for a given field name.
     *
     * @param message The Protobuf message
     * @param fieldName The name of the field to get extracted
     * @param fieldClass The class of the field
     * @return The field value
     */
    @Override
    public <F> F extractValue(final Message message, final String fieldName, final Class<F> fieldClass) {
        log.trace("Record value of type Protobuf Message");

        final FieldDescriptor fieldDescriptor = message.getDescriptorForType().findFieldByName(fieldName);
        if (fieldDescriptor == null) {
            final String errorMessage = String.format("Could not find range field with name %s", fieldName);
            throw new MirrorTopologyException(errorMessage);
        }

        final Object rangeFieldValue = message.getField(fieldDescriptor);
        log.trace("Extracted range field value is: {}", rangeFieldValue);
        return fieldClass.cast(rangeFieldValue);
    }
}
