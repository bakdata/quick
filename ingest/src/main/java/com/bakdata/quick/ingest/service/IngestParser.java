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

package com.bakdata.quick.ingest.service;

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

/**
 * Custom JSON parser for parsing incoming data.
 *
 * <p>
 * This class allows parsing data based on the types in the registry. Additionally, it can work with a single object as
 * well as an array.
 */
@Singleton
public class IngestParser {
    private final ObjectMapper objectMapper;

    @Inject
    public IngestParser(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a single or multiple keys.
     *
     * @param payload   object or arrays of keys
     * @param topicData the registry's topicData about the topic
     * @param <K>       type of the key
     * @return list of keys
     * @throws IOException Jackson JSON error
     */
    public <K> List<K> parseKeyData(final String payload, final QuickTopicData<K, ?> topicData) throws IOException {
        final TypeResolver<K> resolver = topicData.getKeyData().getResolver();
        try (final JsonParser parser = this.objectMapper.getFactory().createParser(payload)) {
            final JsonNode jsonNode = parser.readValueAsTree();
            if (jsonNode.isArray()) {
                final List<K> pairs = new ArrayList<>();
                final Iterator<JsonNode> elements = jsonNode.elements();
                while (elements.hasNext()) {
                    pairs.add(parse(resolver, elements.next()));
                }
                return pairs;
            }
            return List.of(parse(resolver, jsonNode));
        }
    }

    /**
     * Parses an object or array of key value pairs into a list.
     *
     * @param payload   the raw key value pairs as json, either a single one or an array
     * @param topicData the registry's topicData about the topic
     * @param <K>       type of the key
     * @param <V>       type of the value
     * @return list of parsed key value pairs
     * @throws IOException Jackson JSON error
     */
    public <K, V> List<KeyValuePair<K, V>> parseInputData(final String payload, final QuickTopicData<K, V> topicData)
        throws IOException {
        final JsonNode jsonNode;
        try (final JsonParser parser = this.objectMapper.getFactory().createParser(payload)) {
            jsonNode = parser.readValueAsTree();
            if (jsonNode.isObject()) {
                return List.of(fromJsonNode(jsonNode, topicData));
            }

            if (jsonNode.isArray()) {
                final List<KeyValuePair<K, V>> pairs = new ArrayList<>();
                final Iterator<JsonNode> elements = jsonNode.elements();
                while (elements.hasNext()) {
                    pairs.add(fromJsonNode(elements.next(), topicData));
                }
                return pairs;
            }

            throw new BadArgumentException(
                "Expected key-value object or list of key-value objects. Got: " + jsonNode.getNodeType());
        }
    }

    /**
     * Parses a single key value object.
     *
     * @param jsonNode node to parse
     * @param data     the registry's data about the topic
     * @param <K>      type of the key
     * @param <V>      type of the value
     * @return parsed key value pair
     */
    private static <K, V> KeyValuePair<K, V> fromJsonNode(final JsonNode jsonNode, final QuickTopicData<K, V> data) {
        final JsonNode key = jsonNode.get("key");
        final JsonNode value = jsonNode.get("value");
        if (key == null || value == null) {
            throw new BadArgumentException(String.format("Could not find 'key' or 'value' fields in: %s", jsonNode));
        }
        final TypeResolver<K> keyResolver = data.getKeyData().getResolver();
        final TypeResolver<V> valueResolver = data.getValueData().getResolver();
        return new KeyValuePair<>(parse(keyResolver, key), parse(valueResolver, value));
    }

    private static <T> T parse(final TypeResolver<T> typeResolver, final JsonNode node) {
        // the type resolver doesn't check for the JSON node type but we can always create string
        // thus, in case of string type in the topic, we manually check that the node type isn't numeric
        if (node.isNumber() && typeResolver.getType() == QuickTopicType.STRING) {
            final String message = String.format("Data must be of type string. Got: %s (%s)",
                node.getNodeType().toString().toLowerCase(), node);
            throw new BadArgumentException(message);
        }

        try {
            return typeResolver.fromString(node.toString());
        } catch (final AvroConversionException exception) {
            final String errorMessage =
                String.format("Data does not conform to schema: %s", exception.getCause().getMessage());
            throw new BadArgumentException(errorMessage);
        } catch (final RuntimeException exception) {
            final String errorMessage = String.format("Data must be of type %s. Got: %s (%s)",
                typeResolver.getType().toString().toLowerCase(), node.getNodeType().toString().toLowerCase(), node);
            throw new BadArgumentException(errorMessage);
        }
    }
}
