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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
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
 * @param <T> type to return
 * @see PropertyDataFetcher
 * @see TrivialDataFetcher
 */
public class QuickPropertyDataFetcher<T> implements TrivialDataFetcher<T> {
    private final String fieldName;
    private final PropertyDataFetcher<T> propertyDataFetcher;

    public QuickPropertyDataFetcher(final String fieldName) {
        this.fieldName = fieldName;
        this.propertyDataFetcher = new PropertyDataFetcher<>(fieldName);
    }

    @Override
    public T get(final DataFetchingEnvironment environment) {
        if (environment.getSource() instanceof GenericRecord) {
            return this.extractFieldFromRecord(environment.getSource());
        }

        if (environment.getSource() instanceof Message) {
            return this.extractFromMessage(environment);
        }
        return this.propertyDataFetcher.get(environment);
    }

    // save casting because type safety should be enforced by the GraphQL schema
    @SuppressWarnings("unchecked")
    private T extractFromMessage(final DataFetchingEnvironment environment) {
        final Message source = environment.getSource();
        final Descriptors.FieldDescriptor fieldByName = source.getDescriptorForType().findFieldByName(this.fieldName);
        return (T) source.getField(fieldByName);
    }

    // save casting because type safety should be enforced by the GraphQL schema
    @SuppressWarnings("unchecked")
    private T extractFieldFromRecord(final GenericRecord genericRecord) {
        return (T) genericRecord.get(this.fieldName);
    }

}
