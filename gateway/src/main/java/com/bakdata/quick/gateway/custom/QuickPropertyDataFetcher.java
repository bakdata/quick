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

package com.bakdata.quick.gateway.custom;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.TrivialDataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * DataFetcher that extracts a value of a given field from Json.
 */
public class QuickPropertyDataFetcher implements TrivialDataFetcher<Object> {

    private final String fieldName;

    public QuickPropertyDataFetcher(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public Object get(final DataFetchingEnvironment environment) {
        final JsonNode source = environment.getSource();
        final JsonNode fieldValueJson = source.get(fieldName);
        return this.extractValueOfTypeFromJsonNode(fieldValueJson);
    }

    private Object extractValueOfTypeFromJsonNode(final JsonNode fieldValueJson) {
        if (fieldValueJson.isInt()) {
            return fieldValueJson.asInt();
        } else if (fieldValueJson.isDouble()) {
            return fieldValueJson.asDouble();
        } else if (fieldValueJson.isBoolean()) {
            return fieldValueJson.asBoolean();
        } else if (fieldValueJson.isArray() || fieldValueJson.isObject()) {
            return fieldValueJson;
        } else {
            return fieldValueJson.textValue();
        }
    }
}
