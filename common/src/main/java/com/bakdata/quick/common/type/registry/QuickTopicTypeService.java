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

import com.bakdata.quick.common.api.client.mirror.TopicRegistryClient;
import com.bakdata.quick.common.api.model.KeyValueEnum;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.schema.SchemaFetcher;
import com.bakdata.quick.common.type.ConversionProvider;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final AsyncLoadingCache<String, QuickTopicData<?, ?>> cache;
    private final ConversionProvider conversionProvider;

    /**
     * Default constructor.
     *
     * @param registryFetcher http client for schema registry
     * @param topicRegistryClient http client for topic registry
     * @param conversionProvider provider for conversion operations
     */
    public QuickTopicTypeService(final SchemaFetcher registryFetcher,
        final TopicRegistryClient topicRegistryClient,
        final ConversionProvider conversionProvider) {
        this.registryFetcher = registryFetcher;
        this.topicRegistryClient = topicRegistryClient;
        this.conversionProvider = conversionProvider;
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
        log.debug("No cached entry for topic {}", key);
        return this.topicRegistryClient.getTopicData(key)
            .flatMap(this::fromTopicData)
            .as(single -> singleToFuture(executor, single));
    }

    private <K, V> Single<QuickTopicData<K, V>> fromTopicData(final TopicData topicData) {
        final QuickTopicType keyType = topicData.getKeyType();
        final QuickTopicType valueType = topicData.getValueType();

        final Serde<K> keySerde = this.conversionProvider.getSerde(keyType, true);
        final Serde<V> valueSerde = this.conversionProvider.getSerde(valueType, false);

        final String topic = topicData.getName();
        final Single<QuickData<K>> keyData = this.createData(keyType, keySerde, topic, KEY);
        final Single<QuickData<V>> valueData = this.createData(valueType, valueSerde, topic, VALUE);

        // combine key and value data when both are ready
        return keyData.zipWith(valueData,
            (key, value) -> new QuickTopicData<>(topic, topicData.getWriteType(), key, value)
        );
    }

    private <T> Single<QuickData<T>> createData(final QuickTopicType quickTopicType, final Serde<T> serde,
        final String topic, final KeyValueEnum keyValueEnum) {
        final Single<TypeResolverWithSchema<T>> valueResolver =
            this.createResolver(quickTopicType, keyValueEnum.asSubject(topic));
        return valueResolver.map(resolverWithSchema -> new QuickData<>(quickTopicType, serde,
            resolverWithSchema.getTypeResolver(),
            resolverWithSchema.getParsedSchema()));
    }

    private <K> Single<TypeResolverWithSchema<K>> createResolver(final QuickTopicType type, final String subject) {
        // no need for configuration if handle non-schema types
        if (!type.isSchema()) {
            return Single.just(new TypeResolverWithSchema<>(this.conversionProvider.getTypeResolver(type, null),
                null));
        }
        // get schema and configure the resolver with it
        return this.registryFetcher.getSchema(subject)
            .doOnError(e -> log.error("No schema found for subject {}", subject, e))
            .map(schema -> new TypeResolverWithSchema<>(this.conversionProvider.getTypeResolver(type, schema), schema));
    }
}
