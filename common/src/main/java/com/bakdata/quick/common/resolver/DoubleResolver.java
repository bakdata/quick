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
 * Resolver for native double.
 */
public class DoubleResolver implements TypeResolver<Double> {
    private final JavaType elementType;

    public DoubleResolver() {
        this.elementType = TypeFactory.defaultInstance().constructType(Double.class);
    }

    @Override
    public Double fromString(final String value) {
        return Double.valueOf(value);
    }

    @Override
    public String toString(final Double value) {
        return value.toString();
    }

    @Override
    public QuickTopicType getType() {
        return QuickTopicType.DOUBLE;
    }

    @Override
    public JavaType getElementType() {
        return this.elementType;
    }

    @Override
    public Double fromObject(final Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        }
        throw new RuntimeException(String.format("The requested type of %s is not Double.", obj.getClass().getName()));
    }
}
