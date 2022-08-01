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

package com.bakdata.quick.gateway.ingest;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Ingestion service for Kafka.
 */
@Singleton
@Slf4j
public class KafkaIngestService implements IngestService {
    /**
     * The retention time of the cache used for the kafka producers.
     *
     * <p>
     * For now, it is not possible to invalidate a producer when a topic is deleted. Therefore, we can not set the
     * retention time too high as a topic might have an incompatible kafka producer.
     */
    private static final int PRODUCER_CACHE_RETENTION = 2;

    private final Properties properties;
    private final TopicTypeService typeService;
    private final Cache<String, Producer<?, ?>> producerMap;
    private final Scheduler threadPool;

    /**
     * Injectable constructor.
     *
     * @param typeService service for getting topic types
     * @param kafkaConfig configuration for kafka
     */
    @Inject
    public KafkaIngestService(final TopicTypeService typeService, final KafkaConfig kafkaConfig) {
        this.typeService = typeService;
        this.properties = new Properties();
        this.properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
        this.properties.setProperty("schema.registry.url", kafkaConfig.getSchemaRegistryUrl());
        this.producerMap = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(PRODUCER_CACHE_RETENTION)) // keep if used recently
            .build();
        this.threadPool = Schedulers.from(Executors.newWorkStealingPool());
    }

    @Override
    public <K, V> Completable sendData(final String topic, final List<KeyValuePair<K, V>> keyValuePairs) {
        log.debug("Sending data to topic: {}", topic);
        final Single<QuickTopicData<K, V>> topicInformation = this.typeService.getTopicData(topic);
        return topicInformation.flatMapCompletable(info -> this.sendBatchData(topic, keyValuePairs, info))
            .subscribeOn(this.threadPool);
    }

    @Override
    public <K> Completable deleteData(final String topic, final List<K> keys) {
        // How to delete records: https://www.confluent.io/blog/handling-gdpr-log-forget/
        final List<KeyValuePair<K, Void>> pairs = keys.stream()
            .map(key -> new KeyValuePair<K, Void>(key, null))
            .collect(Collectors.toList());
        final Single<QuickTopicData<K, Void>> topicInformation = this.typeService.getTopicData(topic);
        return topicInformation
            .flatMapCompletable(info -> this.sendBatchData(topic, pairs, info))
            .subscribeOn(this.threadPool);
    }

    private <K, V> Completable sendBatchData(final String topic, final List<KeyValuePair<K, V>> data,
        final QuickTopicData<K, V> info) {
        final Serializer<K> keySerializer = info.getKeyData().getSerde().serializer();
        final Serializer<V> valueSerializer = info.getValueData().getSerde().serializer();
        final Producer<K, V> producer = this.getProducer(topic, keySerializer, valueSerializer);
        final Collection<Completable> futures = new ArrayList<>();
        for (final KeyValuePair<K, V> pair : data) {
            final ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, pair.getKey(), pair.getValue());
            final Future<RecordMetadata> metadataFuture = producer.send(producerRecord);
            futures.add(Completable.fromFuture(metadataFuture));
        }

        return Completable.mergeDelayError(futures);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Producer<K, V> getProducer(final String topic, final Serializer<K> keySerializer,
        final Serializer<V> valueSerializer) {
        // producer are closed in post construct
        return (Producer<K, V>) this.producerMap
            .get(topic, topicName -> this.createProducer(keySerializer, valueSerializer, topicName));
    }

    private <K, V> Producer<K, V> createProducer(final Serializer<K> keySerializer,
        final Serializer<V> valueSerializer, final String topic) {
        log.debug("No producer cached for topic {}, creating new one", topic);
        this.properties.setProperty(KEY_SERIALIZER_CLASS_CONFIG, keySerializer.getClass().getName());
        this.properties.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer.getClass().getName());
        return new KafkaProducer<>(this.properties);
    }

    @PreDestroy
    private void tearDown() {
        // close all cached producers and invalidate cache
        log.debug("Teardown ingest: Invalidate producer cache");
        this.producerMap.asMap().values().forEach(Producer::close);
        this.producerMap.invalidateAll();
    }
}
