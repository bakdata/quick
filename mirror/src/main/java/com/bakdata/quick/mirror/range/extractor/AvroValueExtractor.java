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

import com.bakdata.quick.common.exception.MirrorTopologyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;


/**
 * Implements the extraction logic for a Avro's Generic Record.
 *
 * @param <F> Type of the field value
 */
@Slf4j
public class AvroValueExtractor<F> implements RangeFieldValueExtractor<GenericRecord, F> {

    private final Class<F> fieldClass;

    /**
     * Standard constructor.
     *
     * @param fieldClass Class of the field
     */
    public AvroValueExtractor(final Class<F> fieldClass) {
        this.fieldClass = fieldClass;
    }

    /**
     * Extracts the value from an Avro record for a given field name.
     *
     * @param record The Avro record
     * @param rangeField The name of the field to get extracted
     * @return The field value
     */
    @Override
    public F extractValue(final GenericRecord record, final String rangeField) {
        try {
            log.trace("Record value of type Avro Generic Record");
            final Object rangeFieldValue = record.get(rangeField);
            log.trace("Extracted range field value is: {}", rangeFieldValue);
            return this.fieldClass.cast(rangeFieldValue);
        } catch (final AvroRuntimeException exception) {
            final String errorMessage = String.format("Could not find range field with name %s", rangeField);
            throw new MirrorTopologyException(errorMessage);
        }
    }
}
