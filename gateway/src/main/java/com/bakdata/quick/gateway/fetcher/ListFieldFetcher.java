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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.execution.AbortExecutionException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;

/**
 * Fetches multiple values from a mirror.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ListFieldFetcher<K, V> implements DataFetcher<List<V>> {
    private final String idFieldName;
    private final DataFetcherClient<K, V> client;

    public ListFieldFetcher(final String idFieldName, final DataFetcherClient<K, V> client) {
        this.idFieldName = idFieldName;
        this.client = client;
    }

    @Override
    @Nullable
    public List<V> get(final DataFetchingEnvironment environment) {
        final List<K> keys = this.findKeys(environment);
        return this.client.fetchResults(keys);
    }

    @SuppressWarnings("unchecked")
    private List<K> findKeys(final DataFetchingEnvironment environment) {
        // in case of a subscription, we get a generic record directly from the Kafka Consumer
        final List<K> keys;
        if (environment.getSource() instanceof GenericRecord) {
            final GenericRecord genericRecord = environment.getSource();
            keys = (List<K>) genericRecord.get(this.idFieldName);
        } else {
            // otherwise, it's a from a request to a mirror and therefore json, i.e. a map
            final Map<String, Object> source = environment.getSource();
            keys = (List<K>) source.get(this.idFieldName);
        }

        if (keys == null) {
            throw new AbortExecutionException(String.format("No keys for field %s found", this.idFieldName));
        }
        return keys;
    }
}
