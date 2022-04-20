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

package com.bakdata.quick.common.resolver;

import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.databind.JavaType;
import org.apache.avro.Schema;

/**
 * A TypeResolver is used for serializing values dynamically in quick.
 */
public interface TypeResolver<T> {
    T fromString(String value);

    String toString(T value);

    QuickTopicType getType();

    JavaType getElementType();

    T fromObject(Object obj);

    default void configure(final Schema schema) {
        // do nothing
    }
}
