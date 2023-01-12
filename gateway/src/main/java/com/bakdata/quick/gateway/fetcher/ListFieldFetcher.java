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

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.execution.AbortExecutionException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;

/**
 * Fetches multiple values from a mirror.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ListFieldFetcher<K, V> implements DataFetcher<List<V>> {
    private final String fieldName;
    private final DataFetcherClient<K, V> client;

    public ListFieldFetcher(final String fieldName, final DataFetcherClient<K, V> client) {
        this.fieldName = fieldName;
        this.client = client;
    }

    @Override
    @Nullable
    public List<V> get(final DataFetchingEnvironment environment) {
        final List<K> keys = this.findKeys(environment);
        return this.client.fetchResults(keys);
    }

    private List<K> findKeys(final DataFetchingEnvironment environment) {
        // in case of a subscription, we get a generic record directly from the Kafka Consumer
        if (environment.getSource() instanceof GenericRecord) {
            return this.extractGenericRecordValue(environment);
        } else if (environment.getSource() instanceof Message) {
            return this.extractDynamicMessageValue(environment);
        }
        // otherwise, it's a from a request to a mirror and therefore json, i.e. a map
        return this.extractJson(environment);
    }

    @SuppressWarnings("unchecked")
    private List<K> extractGenericRecordValue(final DataFetchingEnvironment environment) {
        try {
            final GenericRecord genericRecord = environment.getSource();
            return (List<K>) genericRecord.get(this.fieldName);
        } catch (final AvroRuntimeException exception) {
            final String errorMessage = String.format("Could not find field with name %s", this.fieldName);
            throw new MirrorTopologyException(errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private List<K> extractDynamicMessageValue(final DataFetchingEnvironment environment) {
        final Message message = environment.getSource();
        final FieldDescriptor fieldDescriptor = message.getDescriptorForType().findFieldByName(
            this.fieldName);
        if (fieldDescriptor == null) {
            throw new AbortExecutionException(String.format("No keys for field %s found", this.fieldName));
        }
        return (List<K>) message.getField(fieldDescriptor);
    }

    @SuppressWarnings("unchecked")
    private List<K> extractJson(final DataFetchingEnvironment environment) {
        final Map<String, Object> source = environment.getSource();
        final List<K> keys = (List<K>) source.get(this.fieldName);
        if (keys == null) {
            throw new AbortExecutionException(String.format("No keys for field %s found", this.fieldName));
        }
        return keys;
    }
}
