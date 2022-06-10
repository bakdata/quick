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

package com.bakdata.quick.common.type.registry;

import static com.bakdata.quick.common.api.model.KeyValueEnum.KEY;
import static com.bakdata.quick.common.api.model.KeyValueEnum.VALUE;

import com.bakdata.quick.common.api.client.TopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.schema.SchemaFetcher;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;

/**
 * Service for getting and configuring topic data from the topic registry.
 */
@Singleton
@Slf4j
public class QuickTopicTypeService implements TopicTypeService {
    private final SchemaFetcher registryFetcher;
    private final TopicRegistryClient topicRegistryClient;
    private final String schemaRegistryUrl;
    private final AsyncLoadingCache<String, QuickTopicData<?, ?>> cache;

    /**
     * Default constructor.
     *
     * @param registryFetcher     http client for schema registry
     * @param topicRegistryClient http client for topic registry
     * @param kafkaConfig         configuration for kafka
     */
    public QuickTopicTypeService(final SchemaFetcher registryFetcher,
        final TopicRegistryClient topicRegistryClient, final KafkaConfig kafkaConfig) {
        this.registryFetcher = registryFetcher;
        this.topicRegistryClient = topicRegistryClient;
        this.schemaRegistryUrl = kafkaConfig.getSchemaRegistryUrl();
        this.cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(Duration.ofSeconds(5))
            .buildAsync(this::loadTopicData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Single<QuickTopicData<K, V>> getTopicData(final String topic) {
        return Single.fromFuture(this.cache.get(topic)).map(data -> (QuickTopicData<K, V>) data);
    }

    @Override
    public Single<QuickTopicType> getKeyType(final String topic) {
        return this.getTopicData(topic).map(QuickTopicData::getKeyData).map(QuickData::getType);
    }

    @Override
    public Single<QuickTopicType> getValueType(final String topic) {
        return this.getTopicData(topic).map(QuickTopicData::getValueData).map(QuickData::getType);
    }

    @SuppressWarnings("unused") // nothing we can do with the disposable; the value will be in the future
    private static CompletableFuture<QuickTopicData<?, ?>> singleToFuture(final Executor executor,
        final Single<QuickTopicData<Object, Object>> single) {
        final CompletableFuture<QuickTopicData<?, ?>> cf = new CompletableFuture<>();
        final Disposable disposable = single.subscribeOn(Schedulers.from(executor))
            .subscribe(cf::complete, cf::completeExceptionally);
        return cf;
    }

    private CompletableFuture<QuickTopicData<?, ?>> loadTopicData(final String key, final Executor executor) {
        log.info("No cached entry for topic {}", key);
        this.cache.asMap().forEach((k, v) -> {
            log.info("{}", k);
        });
        return this.topicRegistryClient.getTopicData(key)
            .flatMap(this::fromTopicData)
            .as(single -> singleToFuture(executor, single));
    }

    private Completable configureResolver(final QuickTopicType type, final String subject,
        final TypeResolver<?> resolver) {
        // no need for configuration if we handle non-avro types
        if (type != QuickTopicType.SCHEMA) {
            return Completable.complete();
        }
        // get schema and configure the resolver with it
        return this.registryFetcher.getSchema(subject)
            .doOnError(e -> log.error("No schema found for subject {}", subject, e))
            .doOnSuccess(resolver::configure)
            .ignoreElement();
    }

    private <K, V> Single<QuickTopicData<K, V>> fromTopicData(final TopicData topicData) {
        final QuickTopicType keyType = topicData.getKeyType();
        final QuickTopicType valueType = topicData.getValueType();

        final Serde<K> keySerde = keyType.getSerde();
        final Serde<V> valueSerde = valueType.getSerde();
        final TypeResolver<K> keyResolver = keyType.getTypeResolver();
        final TypeResolver<V> valueResolver = valueType.getTypeResolver();

        final Map<String, String> configs = Map.of("schema.registry.url", this.schemaRegistryUrl);
        keySerde.configure(configs, true);
        valueSerde.configure(configs, false);

        final String topic = topicData.getName();
        // configure key and value resolver - only required if we handle avro
        final Completable configureResolver = Completable.mergeArray(
            this.configureResolver(keyType, KEY.asSubject(topic), keyResolver),
            this.configureResolver(valueType, VALUE.asSubject(topic), valueResolver)
        );

        final QuickData<K> keyInfo = new QuickData<>(keyType, keySerde, keyResolver);
        final QuickData<V> valueInfo = new QuickData<>(valueType, valueSerde, valueResolver);
        final QuickTopicData<K, V> info = new QuickTopicData<>(topic, topicData.getWriteType(), keyInfo, valueInfo);
        // first we need to configure the resolver, then we can publish the info
        return configureResolver.andThen(Single.just(info));
    }

}