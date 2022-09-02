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

import com.bakdata.quick.common.exception.MirrorException;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.micronaut.http.HttpStatus;
import java.text.DecimalFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class RangeUtils<K, V> {
    private final String rangeField;

    private static final int MAX_INTEGER_LENGTH = 10;
    private static final int MAX_LONG_LENGTH = 19;

    public RangeUtils(final String rangeField) {
        this.rangeField = rangeField;
    }

    public String createRangeIndex(final K key, final V value) {
        final Object rangeFieldValue = this.getRangeFieldValue(value, this.rangeField);
        return String.format("%s_%s", pad(key), pad(rangeFieldValue));
    }

    @NotNull
    private static String pad(final Object number) {
        final String paddedKey;
        if (number instanceof Integer) {
            paddedKey = padZeros(((Integer) number));
        } else if (number instanceof Long) {
            paddedKey = padZeros(((Long) number));
        } else {
            throw new MirrorException(
                "The given key or range field type is not supported for range queries. Supported types for range "
                    + "queries: "
                    + "integer and long",
                HttpStatus.BAD_REQUEST);
        }
        return paddedKey;
    }

    private Object getRangeFieldValue(final V value, final String rangeField) {
        final Object rangeFieldValue;
        if (value instanceof GenericRecord) {
            log.trace("Record value of type Avro Generic Record");
            final GenericRecord genericRecordValue = (GenericRecord) value;
            try {
                rangeFieldValue = genericRecordValue.get(rangeField);
            } catch (final AvroRuntimeException exception) {
                final String message = String.format("Could not find range field with name %s", rangeField);
                throw new MirrorException(message, HttpStatus.BAD_REQUEST);
            }
            log.trace("Extracted range field value is: {}", rangeFieldValue);
        } else if (value instanceof Message) {
            log.trace("Record value of type Protobuf Message");
            final Message messageValue = (Message) value;

            final FieldDescriptor fieldDescriptor =
                messageValue.getDescriptorForType().findFieldByName(rangeField);
            if (fieldDescriptor == null) {
                final String message = String.format("Could not find range field with name %s", rangeField);
                throw new MirrorException(message, HttpStatus.BAD_REQUEST);
            }
            rangeFieldValue = messageValue.getField(fieldDescriptor);
            log.trace("Extracted range field value is: {}", rangeFieldValue);
        } else {
            throw new MirrorException(
                "The value of the topic should be schema (Avro or Protobuf) and not a primitive type",
                HttpStatus.BAD_REQUEST);
        }
        return rangeFieldValue;
    }

    private static String padZeros(final int number) {
        final DecimalFormat decimalFormat = new DecimalFormat("0".repeat(MAX_INTEGER_LENGTH));
        return decimalFormat.format(number);
    }

    private static String padZeros(final long number) {
        final DecimalFormat decimalFormat = new DecimalFormat("0".repeat(MAX_LONG_LENGTH));
        return decimalFormat.format(number);
    }
}
