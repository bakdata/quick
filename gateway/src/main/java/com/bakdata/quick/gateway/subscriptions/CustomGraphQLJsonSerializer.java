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

package com.bakdata.quick.gateway.subscriptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micronaut.configuration.graphql.GraphQLJsonSerializer;
import java.io.IOException;

/**
 * CustomGraphQLJsonSerializer mitigates the problem of returning double-quoted strings from the gateway.
 * This problem arises when a fetcher retrieves a string from a mirror, which is later
 * converted to TextNode. With the default serializer, we obtain a double-quoted string due to serialisation.
 * However, we want to return a simple string to the user, so for strings (TextNodes), we must extract a text value
 * instead of rewriting it as a string, which results in a double-quoted string.
 */
public class CustomGraphQLJsonSerializer implements GraphQLJsonSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     *
     */
    public CustomGraphQLJsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String serialize(final Object object) {
        try {
            if (object instanceof TextNode) {
                return ((TextNode) object).textValue();
            } else {
                return objectMapper.writeValueAsString(object);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error serializing object to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(final String json, final Class<T> requiredType) {
        try {
            return objectMapper.readValue(json, requiredType);
        } catch (final IOException e) {
            throw new RuntimeException("Error deserializing object from JSON: " + e.getMessage(), e);
        }
    }
}
