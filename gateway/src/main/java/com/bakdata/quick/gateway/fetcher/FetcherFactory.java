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

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.subscription.KafkaSubscriptionProvider;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionFetcher;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionProvider;
import com.bakdata.quick.gateway.ingest.KafkaIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import io.reactivex.Single;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * Factory for instantiating the different {@link DataFetcher} used in Quick.
 */
@Singleton
@Slf4j
public class FetcherFactory {
    private final KafkaConfig kafkaConfig;
    private final ObjectMapper objectMapper;
    private final ClientSupplier clientSupplier;
    private final TopicTypeService topicTypeService;


    /**
     * Visible for testing.
     */
    @VisibleForTesting
    public FetcherFactory(final KafkaConfig kafkaConfig, final ObjectMapper objectMapper,
                          final ClientSupplier clientSupplier, final TopicTypeService topicTypeService) {
        this.kafkaConfig = kafkaConfig;
        this.objectMapper = objectMapper;
        this.clientSupplier = clientSupplier;
        this.topicTypeService = topicTypeService;
    }

    /**
     * Constructor used by Micronaut.
     *
     * <p> Parameters are injected.
     */
    @Inject
    public FetcherFactory(final KafkaConfig kafkaConfig,
                          final HttpClient client, final MirrorConfig mirrorConfig,
                          final TopicTypeService topicTypeService) {
        this(kafkaConfig, client.objectMapper(),
            new DefaultClientSupplier(client, topicTypeService, mirrorConfig), topicTypeService);
    }

    public <K, V> DataFetcher<Publisher<V>> subscriptionFetcher(final String topic, final String operationName,
                                                                @Nullable final String argument) {
        final Lazy<QuickTopicData<K, V>> topicData = this.getTopicData(topic);
        return new SubscriptionFetcher<>(this.kafkaConfig, topicData, operationName, argument);
    }

    public <V> DataFetcher<V> queryFetcher(final String topic, final String argument, final boolean isNullable) {
        final DataFetcherClient<V> client = this.clientSupplier.createClient(topic);
        return new QueryKeyArgumentFetcher<>(argument, client, isNullable);
    }

    public <V> DataFetcher<List<V>> queryListFetcher(final String topic, final boolean isNullable,
                                                     final boolean hasNullableElements) {
        final DataFetcherClient<V> client = this.clientSupplier.createClient(topic);
        return new QueryListFetcher<>(client, isNullable, hasNullableElements);
    }

    public <V> DataFetcher<List<V>> listArgumentFetcher(final String topic, final String argument,
                                                        final boolean isNullable, final boolean hasNullableElements) {
        final DataFetcherClient<V> client = this.clientSupplier.createClient(topic);
        return new ListArgumentFetcher<>(argument, client, isNullable, hasNullableElements);
    }

    public <V> DataFetcher<List<V>> rangeFetcher(final String topic, final String argument, final String rangeFrom, final
        String rangeTo, final boolean isNullable) {
        final DataFetcherClient<V> client = this.clientSupplier.createClient(topic);
        return new RangeQueryFetcher<>(argument, client, rangeFrom, rangeTo, isNullable);
    }

    /**
     * Creates a MutationFetcher object.
     *
     * @see MutationFetcher
     */
    public <K, V> DataFetcher<V> mutationFetcher(final String topic, final String keyArgumentName,
                                                 final String valueArgumentName) {
        final Lazy<QuickTopicData<K, V>> data = this.getTopicDataWithTopicTypeService(topic);
        return new MutationFetcher<>(topic,
            keyArgumentName,
            valueArgumentName,
            data,
            new KafkaIngestService(this.topicTypeService, this.kafkaConfig),
            this.objectMapper
        );
    }

    public <V> DataFetcher<List<V>> listFieldFetcher(final String topic, final String keyFieldName) {
        return new ListFieldFetcher<>(keyFieldName, this.clientSupplier.createClient(topic));
    }

    public DataFetcher<Object> keyFieldFetcher(final String topic, final String keyFieldName) {
        return new KeyFieldFetcher<>(this.objectMapper, keyFieldName, this.clientSupplier.createClient(topic));
    }

    public <K, V> SubscriptionProvider<K, V> subscriptionProvider(final String topic, final String operationName,
                                                                  @Nullable final String argument) {
        return new KafkaSubscriptionProvider<>(this.kafkaConfig, this.getTopicData(topic), operationName,
            argument);
    }

    public <V> DataFetcherClient<V> dataFetcherClient(final String topic) {
        return this.clientSupplier.createClient(topic);
    }

    public DataFetcher<DataFetcherResult<Object>> deferFetcher() {
        return new DeferFetcher();
    }

    private <K, V> Lazy<QuickTopicData<K, V>> getTopicData(final String topic) {
        return new Lazy<>(() -> {
            final Single<QuickTopicData<K, V>> topicData = this.topicTypeService.getTopicData(topic);
            return topicData.blockingGet();
        });
    }

    private <K, V> Lazy<QuickTopicData<K, V>> getTopicDataWithTopicTypeService(final String topic) {
        return new Lazy<>(() -> {
            log.debug("requesting topic data from topic {}", topic);
            final Single<QuickTopicData<K, V>> topicData = this.topicTypeService.getTopicData(topic);
            return topicData.blockingGet();
        });
    }

}
