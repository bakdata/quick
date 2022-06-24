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

package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.exception.MirrorException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Custom JSON parser for {@link MirrorValue} using {@link TypeResolver}.
 *
 * @param <V> type of the value
 */
class MirrorValueParser<V> {
    /**
     * Name of the field in {@link MirrorValue} containing the value.
     */
    public static final String FIELD_NAME = "value";
    private final TypeResolver<V> resolver;
    private final ObjectMapper objectMapper;

    MirrorValueParser(final TypeResolver<V> resolver, final ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.objectMapper = objectMapper;
    }

    MirrorValue<V> deserialize(final InputStream inputStream) throws IOException {
        final JsonNode jsonNode = this.objectMapper.readTree(inputStream);
        final JsonNode value = jsonNode.get(FIELD_NAME);
        if (value.isArray()) {
            throw new MirrorException("Expected single value, but got an array", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new MirrorValue<>(this.parseValue(value));
    }

    MirrorValue<List<V>> deserializeList(final InputStream inputStream) throws IOException {
        final JsonNode jsonNode = this.objectMapper.readTree(inputStream);
        final JsonNode valueNode = jsonNode.get(FIELD_NAME);
        if (!valueNode.isArray()) {
            throw new MirrorException("Expected an array, but got a single value", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        final List<V> collect = StreamSupport.stream(valueNode.spliterator(), false)
            .map(this::parseValue)
            .collect(Collectors.toList());
        return new MirrorValue<>(collect);
    }

    private V parseValue(final JsonNode element) {
        // If this is a textualValue, `toString()` returns a string with unwanted quotes
        final String stringValue = element.isTextual() ? element.textValue() : element.toString();
        return this.resolver.fromString(stringValue);
    }

}
