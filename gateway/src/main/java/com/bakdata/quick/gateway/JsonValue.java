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

import com.bakdata.quick.common.exception.InternalErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.Nullable;

public class JsonValue {

    @Nullable
    private final JsonNode jsonNode;

    public JsonValue(@Nullable final JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public static JsonValue fromJsonNode(@Nullable final JsonNode jsonNode) {
        return new JsonValue(jsonNode);
    }

    public Object getValue() {
        if (this.jsonNode != null) {
            if (this.jsonNode.isInt()) {
                return this.jsonNode.asInt();
            } else if (this.jsonNode.isDouble()) {
                return this.jsonNode.asDouble();
            } else if (this.jsonNode.isBoolean()) {
                return this.jsonNode.asBoolean();
            } else if (this.jsonNode.isArray() || this.jsonNode.isObject()) {
                return this.jsonNode;
            } else {
                return this.jsonNode.textValue();
            }
        }
            throw new InternalErrorException("nul");
    }

}
