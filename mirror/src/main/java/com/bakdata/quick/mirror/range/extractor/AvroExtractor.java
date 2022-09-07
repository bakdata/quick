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

import com.bakdata.quick.common.exception.MirrorException;
import io.micronaut.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;

@Slf4j
public class AvroExtractor<F> implements RangeFieldValueExtractor<GenericRecord, F> {

    private final Class<F> fieldClass;

    public AvroExtractor(final Class<F> fieldClass) {
        this.fieldClass = fieldClass;
    }

    @Override
    public F extractValue(final GenericRecord schema, final String rangeField) {
        try {
            log.trace("Record value of type Avro Generic Record");
            final Object rangeFieldValue = schema.get(rangeField);
            log.trace("Extracted range field value is: {}", rangeFieldValue);
            return fieldClass.cast(rangeFieldValue);
        } catch (final AvroRuntimeException exception) {
            final String message = String.format("Could not find range field with name %s", rangeField);
            throw new MirrorException(message, HttpStatus.BAD_REQUEST);
        }
    }
}
