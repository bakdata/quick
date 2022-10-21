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

import com.bakdata.quick.mirror.range.extractor.type.FieldTypeExtractor;
import com.bakdata.quick.mirror.range.padder.ZeroPadder;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import lombok.Getter;

/**
 * An extractor for retrieving values from schemas.
 *
 * @param <V> Type of the schema
 * @param <F> Type of the field in the schema
 */

public abstract class FieldValueExtractor<V, F> {
    protected ZeroPadder<F> zeroPadder;

    protected FieldValueExtractor(ZeroPadder<F> zeroPadder) {
        this.zeroPadder =  zeroPadder;
    }

    public ZeroPadder<F> getZeroPadder() {
        return this.zeroPadder;
    }

    public abstract F extractValue(final V complexValue, final String rangeField);
}
