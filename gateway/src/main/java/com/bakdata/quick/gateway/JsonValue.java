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

package com.bakdata.quick.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A wrapper class over JsonNode which provides a custom way to access the
 * value from the corresponding JsonNode.
 */
public class JsonValue {

    @Nullable
    private final JsonNode jsonNode;

    private JsonValue(@Nullable final JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    /**
     * Static factory method for creating instances of JsonValue from a JsonNode.
     *
     * @param jsonNode a JsonNode that forms a basis for the JsonValue
     * @return an instance of JsonValue
     */
    public static JsonValue fromJsonNode(@Nullable final JsonNode jsonNode) {
        return new JsonValue(jsonNode);
    }

    /**
     * Extracts a value from the corresponding JsonNode conforming to
     * the underlying type of the value. Values can have different types
     * so an instance of Object must be returned to cover all possible types.
     *
     * @return an object that represents the value of the jsonNode
     */
    @Nullable
    public Object fetchValue() {
        if (this.jsonNode != null) {
            if (this.jsonNode.isInt()) {
                return this.jsonNode.asInt();
            } else if (this.jsonNode.isDouble()) {
                return this.jsonNode.asDouble();
            } else if (this.jsonNode.isLong()) {
                return this.jsonNode.asLong();
            } else if (this.jsonNode.isBoolean()) {
                return this.jsonNode.asBoolean();
            } else if (this.jsonNode.isArray() || this.jsonNode.isObject()) {
                return this.jsonNode;
            } else {
                return this.jsonNode.textValue();
            }
        }
        return null;
    }

    /**
     * Extracts the values from the corresponding JsonNodes
     * @param nodes a list of JsonNode
     * @return a list of objects where each corresponds to a value from the JsonNode.
     */
    public static List<Object> fetchValuesFromJsonNodes(@Nullable final List<JsonNode> nodes) {
        return Objects.requireNonNull(nodes).stream()
            .map(node -> JsonValue.fromJsonNode(node).fetchValue())
            .collect(Collectors.toList());
    }
}
