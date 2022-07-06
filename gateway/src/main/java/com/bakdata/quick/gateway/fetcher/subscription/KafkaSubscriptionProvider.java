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

package com.bakdata.quick.gateway.fetcher.subscription;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * A subscription provider for Kafka topics.
 *
 * <p>
 * This creates a consumer for a given Kafka topic. It will push all elements written into this topic into the
 * subscription.
 *
 * <p>
 * The implementation relies on {@link KafkaReceiver}, a reactive Kafka consumer. The receiver does not
 * support multiple subscriber. Therefore, the results emitted by the receiver must be published as a hot observable.
 *
 * @param <K> kafka record's key type
 * @param <V> kafka record's value type
 */
@Slf4j
public class KafkaSubscriptionProvider<K, V> implements SubscriptionProvider<K, V> {

    private static final OffsetStrategy DEFAULT_AUTO_OFFSET = OffsetStrategy.LATEST;
    private final KafkaConfig kafkaConfig;
    @Nullable
    private final String key;
    private final Lazy<QuickTopicData<K, V>> info;
    private final String queryName;
    private final OffsetStrategy autoOffset;
    private final AtomicInteger numberSubscriber;
    // publisher emitting values for subscription
    // initialized when subscription is first executed (which is later than applying the definition)
    // TODO: cache correctly; should be aware of key (move key selection into fetcher)
    @Nullable
    private Flux<ConsumerRecord<K, V>> publisher;


    /**
     * Creates a new SubscriptionFetcher.
     *
     * @param kafkaConfig settings concerning bootstrap server and schema registry url
     * @param info        topic information
     * @param queryName   name of the query the subscription is for
     * @param autoOffset  offset strategy
     * @param key         key to filter on - can be null.
     */
    public KafkaSubscriptionProvider(final KafkaConfig kafkaConfig, final Lazy<QuickTopicData<K, V>> info,
        final String queryName, final OffsetStrategy autoOffset, @Nullable final String key) {
        this.kafkaConfig = kafkaConfig;
        this.key = key;
        this.info = info;
        this.queryName = queryName;
        this.autoOffset = autoOffset;
        this.numberSubscriber = new AtomicInteger();
        this.publisher = null;
    }

    public KafkaSubscriptionProvider(final KafkaConfig kafkaConfig, final Lazy<QuickTopicData<K, V>> info,
        final String queryName, @Nullable final String key) {
        this(kafkaConfig, info, queryName, DEFAULT_AUTO_OFFSET, key);
    }

    @Override
    public Flux<ConsumerRecord<K, V>> getElementStream(final DataFetchingEnvironment environment) {
        // this method gets called for each new session, therefore we create the publisher once and then use it
        if (this.publisher != null && this.numberSubscriber.get() > 0) {
            log.debug("Use active publisher for query {}", this.queryName);
            return this.publisher;
        }

        // setup properties for consumer
        final Properties fetchingProps = new Properties();
        fetchingProps.putAll(this.kafkaConfig.asProps());
        fetchingProps.setProperty(CommonClientConfigs.GROUP_ID_CONFIG,
            "subscription-" + this.queryName + "-" + System.currentTimeMillis());
        fetchingProps.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, getDeserializerName(this.info.get().getKeyData()));
        fetchingProps.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, getDeserializerName(this.info.get().getValueData()));
        fetchingProps.setProperty(AUTO_OFFSET_RESET_CONFIG, this.autoOffset.toString());
        final String topic = this.info.get().getName();

        // create a reactive kafka receiver. The used implementation handles a lot of non-trivial stuff like
        // multi-threaded access and rebalancing operations.
        final ReceiverOptions<K, V> options = ReceiverOptions.<K, V>create(fetchingProps)
            .subscription(List.of(topic));
        Flux<ConsumerRecord<K, V>> recordFlux = KafkaReceiver.create(options)
            .receiveAutoAck()
            .flatMap(flux -> flux);

        // handle filtering based on keys
        if (this.key != null) {
            final Object requestedKeyValue = Optional.ofNullable(environment.getArgument(this.key))
                .orElseThrow(() -> new IllegalArgumentException("Could not get argument"));

            recordFlux = recordFlux.filter(consumerRecord -> consumerRecord.key().equals(requestedKeyValue));
        }
        // Do NOT cache the results of the publisher
        // this may lead to sending elements downstream multiple times if a subscriber renews the subscription
        final Flux<ConsumerRecord<K, V>> consumerRecordFlux = recordFlux
            .filter(this::removeNull)
            .subscribeOn(Schedulers.single()); // run on a different thread since polling blocks

        // this allows us to share the publisher between multiple subscribers
        // only if there are 0 reference counts, it gets canceled
        final Flux<ConsumerRecord<K, V>> hotPublisher = consumerRecordFlux.publish(1).refCount();

        // we manually track the number of subscribers to ensure we don't accidentally reuse canceled flux
        this.publisher = hotPublisher
            .doOnSubscribe(sub -> this.addSubscriber())
            .doFinally(reason -> this.removeSubscriber());

        // Run hot publisher
        return this.publisher;
    }

    private static String getDeserializerName(final QuickTopicData.QuickData<?> quickData) {
        return quickData.getSerde().deserializer().getClass().getName();
    }

    private void removeSubscriber() {
        final int updatedSubscriber = this.numberSubscriber.decrementAndGet();
        log.debug("Removed subscriber: Query {} has {} subscriber", this.queryName, updatedSubscriber);
    }

    private void addSubscriber() {
        final int updatedSubscriber = this.numberSubscriber.incrementAndGet();
        log.debug("New subscriber: Query {} has {} subscriber", this.queryName, updatedSubscriber);
    }

    private boolean removeNull(final ConsumerRecord<K, V> consumerRecord) {
        if (consumerRecord == null || consumerRecord.key() == null || consumerRecord.value() == null) {
            log.warn("null in query {}", this.queryName);
            return false;
        }
        return true;
    }

    /**
     * Offset to use when starting a new subscription.
     */
    public enum OffsetStrategy {
        LATEST,
        EARLIEST;

        @Override
        public String toString() {
            // with that it can be used in Kafka configs
            return super.toString().toLowerCase();
        }
    }

}
