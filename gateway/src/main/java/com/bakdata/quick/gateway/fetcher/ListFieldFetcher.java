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

package com.bakdata.quick.gateway.fetcher;

import com.bakdata.quick.gateway.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.execution.AbortExecutionException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;

/**
 * Fetches multiple values from a mirror.
 *
 * @param <T> key type
 */
public class ListFieldFetcher<T> implements DataFetcher<List<Object>> {
    private final String idFieldName;
    private final DataFetcherClient<JsonNode> client;

    public ListFieldFetcher(final String idFieldName, final DataFetcherClient<JsonNode> client) {
        this.idFieldName = idFieldName;
        this.client = client;
    }

    @Override
    @Nullable
    public List<Object> get(final DataFetchingEnvironment environment) {
        final List<String> keys = this.findKeys(environment)
            .stream()
            .map(Object::toString)
            .collect(Collectors.toList());
        final List<JsonNode> nodesFromMirror = this.client.fetchResults(keys);
        return JsonValue.fetchValuesFromJsonNodes(nodesFromMirror);
    }

    @SuppressWarnings("unchecked")
    private List<T> findKeys(final DataFetchingEnvironment environment) {
        // in case of a subscription, we get a generic record directly from the Kafka Consumer
        final List<T> keys;
        if (environment.getSource() instanceof GenericRecord) {
            final GenericRecord genericRecord = environment.getSource();
            keys = (List<T>) genericRecord.get(this.idFieldName);
        } else {
            // otherwise, it's a from a request to a mirror and therefore json, i.e. a map
            final Map<String, Object> source = environment.getSource();
            keys = (List<T>) source.get(this.idFieldName);
        }

        if (keys == null) {
            throw new AbortExecutionException(String.format("No keys for field %s found", this.idFieldName));
        }

        return keys;
    }
}
