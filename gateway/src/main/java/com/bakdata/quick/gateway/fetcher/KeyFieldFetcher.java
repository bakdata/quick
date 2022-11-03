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

import com.bakdata.quick.common.exception.BadArgumentException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DynamicMessage;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.avro.generic.GenericRecord;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

/**
 * A Data Fetcher that resolves a field from a previous request.
 *
 * <p>
 * Consider the following schema:
 * <pre>{@code
 *   type Query {
 *       findPurchase(purchaseId: ID): Purchase @topic(name: "purchase", keyArgument: "purchaseId")
 *   }
 *
 *   type Purchase {
 *       purchaseId: ID
 *       productId: ID
 *       product: Product @topic(name: "product", keyField: "productId")
 *   }
 *
 *   type Product {
 *       ...
 *   }
 * }</pre>
 *
 * <p>
 * First, the data fetcher connected with findPurchase is called and returns a Purchase object with a missing product
 * since it is stored in a different topic. The KeyFieldFetcher extracts the productId from the returned purchase and
 * fetches the corresponding product.
 */
public class KeyFieldFetcher<K, V> implements DataFetcher<Object> {
    private final ObjectMapper objectMapper;
    private final String argument;
    private final DataFetcherClient<K, V> client;
    private final JsonAvroConverter converter;

    /**
     * Constructor.
     *
     * @param objectMapper json handler
     * @param argument name of the argument to extract key from
     * @param client underlying HTTP mirror client
     */
    public KeyFieldFetcher(final ObjectMapper objectMapper,
        final String argument,
        final DataFetcherClient<K, V> client) {
        this.objectMapper = objectMapper;
        this.argument = argument;
        this.client = client;
        this.converter = new JsonAvroConverter();
    }

    @Override
    @Nullable
    public Object get(final DataFetchingEnvironment environment) {
        final List<K> keyArguments = this.findKeyArgument(environment).collect(Collectors.toList());

        // the modification applies either to an array node or to a single field
        // TODO create two different classes for both use cases and create them based on the schema
        if (keyArguments.size() == 1) {
            return this.client.fetchResult(keyArguments.get(0));
        } else {
            return this.client.fetchResults(keyArguments);
        }
    }

    // TODO: Fix this
    private Stream<K> findKeyArgument(final DataFetchingEnvironment environment) {
        final JsonNode parentJson;
        try {
            parentJson = this.extractJson(environment);
        } catch (final IOException e) {
            throw new RuntimeException("Could not convert source for extracting key", e);
        }
        // parse json and try to find the value for our argument
        // if it is an array, we need to resolve each one
        final JsonNode node = parentJson.findValue(this.argument);
        if (node == null) {
            throw new IllegalArgumentException(
                String.format("Field + %s could not be found in source.", this.argument));
        }
        if (node.isArray()) {
            final Iterator<JsonNode> elements = node.elements();
            return (Stream<K>) StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(elements, 0), false)
                .map(KeyFieldFetcher::extractCorrectType);
        } else {
            final Object typedNode = extractCorrectType(node);
            return (Stream<K>) Stream.of(typedNode);
        }
    }

    private static Object extractCorrectType(final JsonNode jsonNode) {
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        } else if (jsonNode.isDouble()) {
            return jsonNode.asDouble();
        } else if (jsonNode.isLong()) {
            return jsonNode.asLong();
        } else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else if (jsonNode.isArray() || jsonNode.isObject()) {
            return jsonNode;
        } else if (jsonNode.isTextual()) {
            return jsonNode.textValue();
        } else {
            throw new BadArgumentException("Provided argument is not supported.");
        }
    }

    private JsonNode extractJson(final DataFetchingEnvironment environment) throws IOException {
        // TODO work on JSON everywhere:
        //  1. Do not convert back to real types in MirrorDataFetcherClient
        //  2. Immediately convert to JSON in SubscriptionFetcher
        if (environment.getSource() instanceof GenericRecord) {
            final GenericRecord record = environment.getSource();
            return this.objectMapper.readTree(this.converter.convertToJson(record));
        }

        if (environment.getSource() instanceof DynamicMessage) {
            final DynamicMessage source = environment.getSource();
            return this.objectMapper.readTree(source.toByteArray());
        }

        final Map<String, Object> value = environment.getSource();
        return this.objectMapper.valueToTree(value);
    }
}
