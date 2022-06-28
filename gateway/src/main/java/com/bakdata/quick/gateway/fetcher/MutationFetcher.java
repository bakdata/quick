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

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.ingest.KafkaIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * The data fetcher here is responsible for executing the mutation, which in our case is ingesting the data into the
 * topic. According to the GraphQL documents the mutation should return the newly updated values. See <a
 * href="https://graphql.org/learn/queries/#mutations">GraphQL Mutations</a>
 */
@Slf4j
public class MutationFetcher<K, V> implements DataFetcher<V> {

    private final String topic;
    private final String keyInputArgumentName;
    private final String valueInputArgumentName;
    private final Lazy<QuickTopicData<K, V>> topicData;
    private final KafkaIngestService kafkaIngestService;
    private final ObjectMapper objectMapper;

    /**
     * Default Constructor.
     *
     * @param topic                  name of the topic to ingest data
     * @param keyInputArgumentName   name of the key argument
     * @param valueInputArgumentName name of the value argument
     * @param quickTopicData         topic information
     * @param kafkaIngestService     A Kafka service to ingest data into the topic
     */
    public MutationFetcher(final String topic,
                           final String keyInputArgumentName,
                           final String valueInputArgumentName,
                           final Lazy<QuickTopicData<K, V>> quickTopicData,
                           final KafkaIngestService kafkaIngestService,
                           final ObjectMapper objectMapper) {

        this.topic = topic;
        this.keyInputArgumentName = keyInputArgumentName;
        this.valueInputArgumentName = valueInputArgumentName;
        this.topicData = quickTopicData;
        this.kafkaIngestService = kafkaIngestService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Nullable
    public V get(final DataFetchingEnvironment environment) throws Exception {
        log.debug("Incoming request: Ingest payload for topic {}", this.topic);

        final Optional<?> keyInputArgument = DeferFetcher.getArgument(this.keyInputArgumentName, environment);
        final Optional<?> valueInputArgument = DeferFetcher.getArgument(this.valueInputArgumentName, environment);

        if (keyInputArgument.isEmpty() || valueInputArgument.isEmpty()) {
            throw GraphqlErrorException.newErrorException()
                .message("key input in the mutation field should not be empty")
                .sourceLocation(environment.getFieldDefinition().getArgument(this.keyInputArgumentName).getDefinition()
                    .getSourceLocation())
                .build();
        }

        // We only support conversion from String and therefore have to convert it back
        final Object rawObject = valueInputArgument.get();
        final String value;
        if (rawObject instanceof Number || rawObject instanceof String) {
            value = rawObject.toString();
        } else {
            value = this.objectMapper.writeValueAsString(rawObject);
        }
        final V resolvedValue = this.topicData.get().getValueData().getResolver().fromString(value);

        final KeyValuePair<?, ?> keyValuePair = new KeyValuePair<>(keyInputArgument.get(), resolvedValue);

        final Throwable throwable = this.kafkaIngestService.sendData(this.topic, List.of(keyValuePair)).blockingGet();

        if (throwable != null) {
            throw new RuntimeException(throwable);
        }

        return resolvedValue;
    }
}
