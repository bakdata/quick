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
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Resolver for long primitives.
 */
public class LongResolver implements TypeResolver<Long> {

    private final JavaType elementType;

    public LongResolver() {
        this.elementType = TypeFactory.defaultInstance().constructType(Long.class);
    }

    @Override
    public Long fromString(final String value) {
        return Long.valueOf(value);
    }

    @Override
    public String toString(final Long value) {
        return value.toString();
    }

    @Override
    public QuickTopicType getType() {
        return QuickTopicType.LONG;
    }

    @Override
    public JavaType getElementType() {
        return this.elementType;
    }

    @Override
    public Long fromObject(final Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        throw new RuntimeException(String.format("The requested type of %s is not Long.", obj.getClass().getName()));
    }

}
