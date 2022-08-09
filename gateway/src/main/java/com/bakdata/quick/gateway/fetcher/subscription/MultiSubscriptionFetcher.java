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

import com.bakdata.quick.gateway.fetcher.DataFetcherClient;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Data fetcher for subscribing to multiple topics.
 *
 * <p>
 * This data fetcher works by subscribing to multiple topics with a {@link KafkaSubscriptionProvider}.
 * When a new query comes in, it subscribes to the Kafka Subscriber for all selected fields.
 * Whenever a subscriber emits an event, the data for the other selected fields is fetched through the mirror.
 */
@Slf4j
public class MultiSubscriptionFetcher implements DataFetcher<Publisher<Map<String, Object>>> {

    public static final int CACHE_SIZE = 5_000;
    private final Map<String, DataFetcherClient<?>> fieldDataFetcherClients;
    /**
     * A cache for all field values.
     *
     * <p>
     * The subscriptions update the values, so that we only need to fetch the values from the mirror in case the
     * kafka didn't yield a value for this field yet.
     */
    private final AsyncLoadingCache<FieldKey<?>, Object> fieldCache;
    private final Map<String, SubscriptionProvider<?, ?>> fieldSubscriptionProviders;
    private final FieldSelector fieldSelector;

    /**
     * Default constructor.
     *
     * @param fieldDataFetcherClients    map of fields to their fetching clients
     * @param fieldSubscriptionProviders map of field to their subscription providers
     */
    public MultiSubscriptionFetcher(final Map<String, DataFetcherClient<?>> fieldDataFetcherClients,
                                    final Map<String, SubscriptionProvider<?, ?>> fieldSubscriptionProviders) {
        this(fieldDataFetcherClients, fieldSubscriptionProviders, MultiSubscriptionFetcher::getSelectedFields);
    }

    /**
     * Constructor with custom field selection for testing purposes.
     *
     * @param fieldDataFetcherClients    map of fields to their fetching clients
     * @param fieldSubscriptionProviders map of field to their subscription providers
     * @param fieldSelector              function extracting the selected fields of a GraphQL environment
     */
    @VisibleForTesting
    MultiSubscriptionFetcher(final Map<String, DataFetcherClient<?>> fieldDataFetcherClients,
                             final Map<String, SubscriptionProvider<?, ?>> fieldSubscriptionProviders,
                             final FieldSelector fieldSelector) {
        this.fieldDataFetcherClients = fieldDataFetcherClients;
        this.fieldSubscriptionProviders = fieldSubscriptionProviders;
        this.fieldSelector = fieldSelector;
        this.fieldCache = Caffeine.newBuilder().maximumSize(CACHE_SIZE).buildAsync(this::loadField);
    }

    @Override
    public Publisher<Map<String, Object>> get(final DataFetchingEnvironment environment) {
        final List<String> selectedFields = this.fieldSelector.selectFields(environment);
        final Flux<? extends NamedRecord<?, ?>> combinedElementsStream =
            this.combineElementStreams(selectedFields, environment);
        return combinedElementsStream.flatMap(namedRecord -> this.createComplexType(namedRecord, selectedFields));
    }

    /**
     * Default implementation of {@link FieldSelector}.
     *
     * <p>
     * Nested types are qualified by their parent and then a /, i.e., parent/child (see
     * {@link graphql.schema.DataFetchingFieldSelectionSet}).
     * In this setting, we're only interested in the first level of fields. Deeper levels are expected to be part of
     * the returned Kafka value itself. Therefore, child wouldn't be part of the returned list.
     *
     * @param environment environment of the current request
     * @return list of root field names returned by this fetcher
     */
    private static List<String> getSelectedFields(final DataFetchingEnvironment environment) {
        return environment.getSelectionSet().getFields().stream()
            .filter(field -> !field.getQualifiedName().contains("/"))
            .map(SelectedField::getName)
            .collect(Collectors.toList());
    }

    /**
     * Adds all values except the one we got from Kafka.
     *
     * <p>
     * There are two cases:
     * <ol>
     *     <il>We already got a value for this field from Kafka: We know this is the latest because of at-least once
     *     guarantees. Therefore we can add it from the cache.</il>
     *     <il>We haven't seen one yet: We need to fetch it from Kafka.
     *     We can also cache it since we get all updates.</il>
     * </ol>
     *
     * @param namedRecord         the record we got from Kafka
     * @param selectedFields the fields selected by this query
     * @return a map representing the selected object
     */
    private Mono<Map<String, Object>> createComplexType(final NamedRecord<?, ?> namedRecord,
                                                        final List<String> selectedFields) {
        // map holding the data for current key
        final Map<String, Object> complexType = new HashMap<>();
        complexType.put(namedRecord.getFieldName(), namedRecord.getConsumerRecord().value());

        final FieldKey<?> key = new FieldKey<>(namedRecord.getFieldName(), namedRecord.getConsumerRecord().key());
        final CompletableFuture<?> recordValue = CompletableFuture.completedFuture(
            namedRecord.getConsumerRecord().value());
        log.info("Update {} with {}", key, namedRecord.getConsumerRecord().value());
        this.fieldCache.put(key, recordValue);

        final Flux<? extends FieldKey<?>> fieldKeysToPopulate = Flux.fromIterable(selectedFields)
            .filter(fieldName -> !fieldName.equals(namedRecord.getFieldName()))
            .map(fieldName -> new FieldKey<>(fieldName, namedRecord.getConsumerRecord().key()));

        final Flux<FieldValue<Object>> fieldValues = fieldKeysToPopulate.flatMap(fieldKey -> {
                log.info("Get key {}", fieldKey);
                return Mono.fromFuture(this.fieldCache.get(fieldKey))
                    .map(value -> {
                        log.info("Set key {} to {}", fieldKey.getFieldName(), value);
                        return new FieldValue<>(fieldKey.getFieldName(), value);
                    });
            }
        );

        return fieldValues.reduce(complexType, (map, fieldValue) -> {
            map.put(fieldValue.getFieldName(), fieldValue.getValue());
            return map;
        });
    }

    @Nullable
    private Object loadField(final FieldKey<?> fieldKey) {
        final DataFetcherClient<?> client = this.fieldDataFetcherClients.get(fieldKey.getFieldName());
        Objects.requireNonNull(client, () -> "No client found for field " + fieldKey.getFieldName());
        return client.fetchResult(fieldKey.getKey().toString());
    }

    private Flux<? extends NamedRecord<?, ?>> combineElementStreams(final List<String> selectedFields,
                                                                    final DataFetchingEnvironment env) {
        final List<Flux<? extends NamedRecord<?, ?>>> fluxes = selectedFields.stream()
            .map(name -> {
                final SubscriptionProvider<?, ?> kafkaSubscriber = this.fieldSubscriptionProviders.get(name);
                Objects.requireNonNull(kafkaSubscriber);
                final Flux<? extends ConsumerRecord<?, ?>> elementStream = kafkaSubscriber.getElementStream(env);
                return elementStream.map(val -> new NamedRecord<>(name, val));
            }).collect(Collectors.toList());
        return Flux.merge(fluxes);
    }

    /**
     * Function extracting the selected fields of a GraphQL environment.
     */
    interface FieldSelector {
        List<String> selectFields(final DataFetchingEnvironment dataFetchingEnvironment);
    }

    @Value
    private static class NamedRecord<K, V> {
        String fieldName;
        ConsumerRecord<K, V> consumerRecord;
    }

    @Value
    private static class FieldKey<K> {
        String fieldName;
        K key;
    }

    @Value
    private static class FieldValue<V> {
        String fieldName;
        V value;
    }
}
