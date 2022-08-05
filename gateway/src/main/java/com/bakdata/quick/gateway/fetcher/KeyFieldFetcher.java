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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A Data Fetcher that resolves a field from a previous request.
 *
 * <p>
 * Consider the following schema:
 * <pre>
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
 * </pre>
 *
 * <p>
 * First, the data fetcher connected with findPurchase is called and returns a Purchase object with a missing product
 * since it is stored in a different topic. The KeyFieldFetcher extracts the productId from the returned purchase and
 * fetches the corresponding product.
 */
public class KeyFieldFetcher implements DataFetcher<JsonNode> {
    private final ObjectMapper objectMapper;
    private final String argument;
    private final DataFetcherClient<JsonNode> client;

    /**
     * Constructor.
     *
     * @param objectMapper json handler
     * @param argument     name of the argument to extract key from
     * @param client       underlying HTTP mirror client
     */
    public KeyFieldFetcher(final ObjectMapper objectMapper, final String argument,
                           final DataFetcherClient<JsonNode> client) {
        this.objectMapper = objectMapper;
        this.argument = argument;
        this.client = client;
    }

    @Override
    @Nullable
    public JsonNode get(final DataFetchingEnvironment environment) {
        final List<String> uriList = this.findKeyArgument(environment).collect(Collectors.toList());
        if (uriList.size() == 1) {
            return this.client.fetchResult(uriList.get(0));
        } else {
            final List<JsonNode> jsonNodes = this.client.fetchResults(uriList);
            return new ArrayNode(JsonNodeFactory.instance, jsonNodes);
        }
    }

    private Stream<String> findKeyArgument(final DataFetchingEnvironment environment) {
        final String parentJson;
        try {
            parentJson = this.extractJson(environment);
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not convert source for extracting key", e);
        }
        // parse json and try to find the value for our argument
        // if it is an array, we need to resolve each one
        try {
            final JsonNode parent = this.objectMapper.readTree(parentJson);
            final JsonNode node = parent.findValue(this.argument);
            if (node == null) {
                throw new IllegalArgumentException(
                    String.format("Field + %s could not be found in source.", this.argument));
            }
            if (node.isArray()) {
                final Iterator<JsonNode> elements = node.elements();
                return StreamSupport.stream(Spliterators.spliteratorUnknownSize(elements, 0), false)
                    .map(this::valueAsString);

            } else {
                return Stream.of(this.valueAsString(node));
            }
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException("Could not process json: " + parentJson, e);
        }
    }

    private String extractJson(final DataFetchingEnvironment environment) throws IOException {
        // TODO work on JSON everywhere:
        //  1. Do not convert back to real types in MirrorDataFetcherClient
        //  2. Immediately convert to JSON in SubscriptionFetcher
        final Map<String, Object> value = environment.getSource();
        return this.objectMapper.writeValueAsString(value);
    }

    private String valueAsString(final JsonNode node) {
        try {
            return node.isTextual() ? node.textValue() : this.objectMapper.writeValueAsString(node);
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException("Could not process json: " + node, e);
        }
    }
}
