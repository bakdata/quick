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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;

/**
 * Type resolver for known types.
 *
 * <p>
 * In contrast to other resolvers, the type of this resolver is known at compile time.
 * For example, the type of the topic-registry topic is known at compile time and is not dynamically created by users.
 *
 * @param <T> type to resolve
 */
public class KnownTypeResolver<T> implements TypeResolver<T> {
    private final Class<T> typeClass;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     *
     * @param typeClass    class of the type to resolve
     * @param objectMapper jackson's object mapper
     */
    public KnownTypeResolver(final Class<T> typeClass, final ObjectMapper objectMapper) {
        this.typeClass = typeClass;
        this.objectMapper = objectMapper;
    }

    @Override
    public T fromString(final String value) {
        try {
            return this.objectMapper.readValue(value, this.typeClass);
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException(String.format("Could not convert \"%s\" into %s", value, this.typeClass), e);
        }
    }

}
