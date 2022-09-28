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

import static org.mockito.Mockito.mock;

import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.directives.topic.TopicDirective;
import com.bakdata.quick.gateway.fetcher.ClientSupplier;
import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

public final class GraphQLTestUtil {
    private GraphQLTestUtil() {
    }

    public static DataFetcher<?> getFieldDataFetcher(final String objectName, final String fieldName,
        final GraphQLSchema schema) {
        final GraphQLFieldDefinition modificationField = getFieldDefinition(objectName, fieldName, schema);
        return schema.getCodeRegistry().getDataFetcher(
            FieldCoordinates.coordinates(objectName, fieldName),
            modificationField
        );
    }

    public static GraphQLFieldDefinition getFieldDefinition(final String objectName, final String fieldName,
        final GraphQLSchema schema) {
        return schema.getObjectType(objectName).getFieldDefinitions()
            .stream()
            .filter(definition -> fieldName.equals(definition.getName()))
            .findFirst()
            .orElseThrow();
    }

    public static List<GraphQLArgument> getTopicDirectiveArgumentsFromField(final String objectName,
        final String fieldName,
        final GraphQLSchema schema) {
        return getFieldDefinition(objectName, fieldName, schema)
            .getDirective(TopicDirective.DIRECTIVE_NAME)
            .getArguments().stream().filter(graphQLArgument -> graphQLArgument.getValue() != null)
            .collect(Collectors.toList());
    }

    static final class TestClientSupplier implements ClientSupplier {
        @Getter
        private final Map<String, DataFetcherClient<?, ?>> clients;

        TestClientSupplier() {
            this.clients = new HashMap<>();
        }

        @Override
        public <K, V> DataFetcherClient<K, V> createClient(final String topic,
            final Lazy<QuickTopicData<K, V>> quickTopicData) {
            final DataFetcherClient<K, V> client = mock(DataFetcherClient.class);
            this.clients.put(topic, client);
            return client;
        }

        @SuppressWarnings("unchecked")
        public <K, V> DataFetcherClient<K, V> getClient(final String client) {
            return (DataFetcherClient<K, V>) this.clients.get(client);
        }
    }
}
