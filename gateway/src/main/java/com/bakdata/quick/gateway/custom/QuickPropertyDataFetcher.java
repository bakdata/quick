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
import graphql.schema.PropertyDataFetcher;
import org.apache.avro.generic.GenericRecord;

/**
 * Data fetcher extending graphql's property fetcher by also handling avro record.
 *
 * <p>
 * The default {@link graphql.schema.DataFetcher} provided by Java GraphQL is the {@link PropertyDataFetcher}. However,
 * it only supports maps and POJOs. This class is an extension allowing handling Avro's {@link GenericRecord}.
 *
 * <p>
 * It first checks whether the field returned by the parent is of type {@link GenericRecord}.
 * If this is not the case, it delegates to {@link PropertyDataFetcher}.
 *
 * @see PropertyDataFetcher
 * @see TrivialDataFetcher
 */
public class QuickPropertyDataFetcher implements TrivialDataFetcher<Object> {

    private final String fieldName;

    public QuickPropertyDataFetcher(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public Object get(final DataFetchingEnvironment environment) {
        final JsonNode source = environment.getSource();
        final JsonNode value = source.get(fieldName);
        return extractValueFromJsonNode(value);
    }

    private Object extractValueFromJsonNode(final JsonNode value) {
        if (value.isInt()) {
            return value.asInt();
        } else if (value.isObject()) {
            return value;
        } else if (value.isDouble()) {
            return value.asDouble();
        } else if (value.isArray()) {
            return value;
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else {
            return value.textValue();
        }
    }
}
