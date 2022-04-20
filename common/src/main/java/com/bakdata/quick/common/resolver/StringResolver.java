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
 * Resolver for strings.
 */
public class StringResolver implements TypeResolver<String> {

    private final JavaType elementType;

    public StringResolver() {
        this.elementType = TypeFactory.defaultInstance().constructType(String.class);
    }

    @Override
    public String fromString(final String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    @Override
    public String toString(final String value) {
        return value;
    }

    @Override
    public QuickTopicType getType() {
        return QuickTopicType.STRING;
    }

    @Override
    public JavaType getElementType() {
        return this.elementType;
    }

    @Override
    public String fromObject(final Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        throw new RuntimeException(String.format("The requested type of %s is not String.", obj.getClass().getName()));
    }
}
