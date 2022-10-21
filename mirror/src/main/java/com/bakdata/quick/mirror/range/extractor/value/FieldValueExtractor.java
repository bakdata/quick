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

/**
 * An extractor for retrieving values from schemas.
 *
 * @param <V> Type of the schema
 */
public interface FieldValueExtractor<V> {
    /**
     * Extracts the value of a field from a complex value (Avro record, or Protobuf message).
     *
     * @param complexValue Avro record or Protobuf message
     * @param fieldName Name of the field
     * @param fieldClass Class of the field
     * @param <F> Type of the field to be extracted
     * @return The value of the field
     */
    <F> F extractValue(final V complexValue, final String fieldName, final Class<F> fieldClass);
}
