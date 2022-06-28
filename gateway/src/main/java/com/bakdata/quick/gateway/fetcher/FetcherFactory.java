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
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.util.KeySerdeValResolverWrapper;
import com.bakdata.quick.common.util.Lazy;
import com.bakdata.quick.gateway.fetcher.subscription.KafkaSubscriptionProvider;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionFetcher;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionProvider;
import com.bakdata.quick.gateway.ingest.KafkaIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.schema.DataFetcher;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;

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

    public DataFetcher<?> subscriptionFetcher(final String topic, final String operationName,
        @Nullable final String argument) {
        return new SubscriptionFetcher<>(this.kafkaConfig, this.getTopicData(topic), operationName,
            argument);
    }

    public DataFetcher<?> queryFetcher(final String topic, final String argument, final boolean isNullable) {
        return new QueryKeyArgumentFetcher<>(argument, this.clientSupplier.createClient(topic), isNullable);
    }

    public DataFetcher<?> queryListFetcher(final String topic, final boolean isNullable,
        final boolean hasNullableElements) {
        return new QueryListFetcher<>(this.clientSupplier.createClient(topic), isNullable, hasNullableElements);
    }

    public DataFetcher<?> listArgumentFetcher(final String topic, final String argument, final boolean isNullable,
        final boolean hasNullableElements) {
        return new ListArgumentFetcher<>(argument, this.clientSupplier.createClient(topic), isNullable,
            hasNullableElements);
    }

    /**
     * Creates a MutationFetcher object.
     *
     * @see MutationFetcher
     */
    public DataFetcher<?> mutationFetcher(final String topic, final String keyArgumentName,
        final String valueArgumentName) {
        return new MutationFetcher<>(topic,
            keyArgumentName,
            valueArgumentName,
            this.getTopicDataWithTopicTypeService(topic),
            new KafkaIngestService(this.topicTypeService, this.kafkaConfig),
            this.objectMapper
        );
    }

    public DataFetcher<?> listFieldFetcher(final String topic, final String keyFieldName) {
        return new ListFieldFetcher<>(keyFieldName, this.clientSupplier.createClient(topic));
    }

    public DataFetcher<?> keyFieldFetcher(final String topic, final String keyFieldName) {
        return new KeyFieldFetcher<>(this.objectMapper, keyFieldName, this.clientSupplier.createClient(topic));
    }

    public SubscriptionProvider<?, ?> subscriptionProvider(final String topic, final String operationName,
        @Nullable final String argument) {
        return new KafkaSubscriptionProvider<>(this.kafkaConfig, this.getTopicData(topic), operationName,
            argument);
    }

    public DataFetcherClient<?> dataFetcherClient(final String topic) {
        return this.clientSupplier.createClient(topic);
    }

    public DataFetcher<?> deferFetcher() {
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


    /**
     * Supplier for creating a new data fetcher client for a topic.
     */
    public interface ClientSupplier {
        DataFetcherClient<?> createClient(final String topic);
    }

    static final class DefaultClientSupplier implements ClientSupplier {
        private final HttpClient client;
        private final TopicTypeService topicTypeService;
        private final MirrorConfig mirrorConfig;

        private DefaultClientSupplier(final HttpClient client, final TopicTypeService topicRegistryClient,
                                      final MirrorConfig mirrorConfig) {
            this.client = client;
            this.topicTypeService = topicRegistryClient;
            this.mirrorConfig = mirrorConfig;
        }

        @Override
        public DataFetcherClient<?> createClient(final String topic) {
            return this.doCreateClient(topic);
        }

        private  <V> DataFetcherClient<V> doCreateClient(final String topic) {
            final Lazy<KeySerdeValResolverWrapper<String, V>> wrapper = this.getKeySerdeValResolverWrapperLazy(topic);
            return new MirrorDataFetcherClient<>(
                    topic,
                    this.client,
                    this.mirrorConfig,
                    wrapper
            );
        }

        private <V> Lazy<KeySerdeValResolverWrapper<String, V>> getKeySerdeValResolverWrapperLazy(final String topic) {
            return new Lazy<>(() -> {
                final Single<QuickTopicData<String, V>> data = this.topicTypeService.getTopicData(topic);
                final QuickTopicData<String, V> topicData = data.blockingGet();
                if (topicData == null) {
                    throw new NotFoundException("Could not find topic " + topic);
                }
                return new KeySerdeValResolverWrapper<>(
                        topicData.getKeyData().getSerde(), topicData.getValueData().getResolver()
                );
            });
        }
    }
}
