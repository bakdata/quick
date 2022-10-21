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
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the extraction logic for a Protobuf message.
 *
 * @param <F> Type of the field value
 */
@Slf4j
public class MessageValueExtractor<F> extends FieldValueExtractor<Message, F> {
    /**
     * Standard constructor.
     *
     * @param fieldClass Class of the field
     */
    public MessageValueExtractor(final ZeroPadder<F> zeroPadder) {
        super(zeroPadder);
    }

    /**
     * Extracts the value from a Protobuf message for a given field name.
     *
     * @param message The Protobuf message
     * @param rangeField The name of the field to get extracted
     * @return The field value
     */
    @Override
    public F extractValue(final Message message, final String rangeField) {
        log.trace("Record value of type Protobuf Message");

        final FieldDescriptor fieldDescriptor = message.getDescriptorForType().findFieldByName(rangeField);
        if (fieldDescriptor == null) {
            final String errorMessage = String.format("Could not find range field with name %s", rangeField);
            throw new MirrorTopologyException(errorMessage);
        }

        final Object rangeFieldValue = message.getField(fieldDescriptor);
        log.trace("Extracted range field value is: {}", rangeFieldValue);
        return this.zeroPadder.getPadderClass().cast(rangeFieldValue);
    }
}
