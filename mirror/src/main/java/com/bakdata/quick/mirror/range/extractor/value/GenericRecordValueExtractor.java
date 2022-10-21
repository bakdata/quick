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
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;


/**
 * Implements the value extraction logic for an Avro's Generic Record.
 */
@Slf4j
public class GenericRecordValueExtractor implements FieldValueExtractor<GenericRecord> {

    /**
     * Extracts the value from an Avro record for a given field name.
     *
     * @param record The Avro record
     * @param fieldName The name of the field to get extracted
     * @param fieldClass The class of the field
     * @return The field value
     */
    @Override
    public <F> F extractValue(final GenericRecord record, final String fieldName, final Class<F> fieldClass) {
        try {
            log.trace("Record value of type Avro Generic Record");
            final Object rangeFieldValue = record.get(fieldName);
            log.trace("Extracted field value is: {}", rangeFieldValue);
            return fieldClass.cast(rangeFieldValue);
        } catch (final AvroRuntimeException exception) {
            final String errorMessage = String.format("Could not find field with name %s", fieldName);
            throw new MirrorTopologyException(errorMessage);
        }
    }
}
