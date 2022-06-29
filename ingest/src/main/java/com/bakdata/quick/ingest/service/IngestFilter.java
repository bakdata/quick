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

import com.bakdata.quick.common.api.client.DefaultMirrorClient;
import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.MirrorClient;
import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.type.QuickTopicData;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ensures immutability of keys for immutable topics.
 */
@Singleton
@Slf4j
public class IngestFilter {
    private final HttpClient client;
    private final MirrorConfig mirrorConfig;

    /**
     * Default constructor.
     *
     * @param client http client
     * @param mirrorConfig config for Quick mirror
     */
    public IngestFilter(final HttpClient client, final MirrorConfig mirrorConfig) {
        this.client = client;
        this.mirrorConfig = mirrorConfig;
    }

    /**
     * Based on the key value pairs to ingest, this methods returns two new lists: one with the keys to ingest, and one
     * with the keys that cannot be overridden.
     *
     * <p>
     * If the topic is mutable, it just returns the original list.
     *
     * @param topicData info of the topics
     * @param pairs     list of kv pairs to ingest
     * @param <K>       key type
     * @param <V>       value type
     * @return two new lists: one with keys to ingest and one with keys that cannot be overriden.
     */
    public <K, V> Single<IngestLists<K, V>> prepareIngest(final QuickTopicData<K, V> topicData,
        final List<KeyValuePair<K, V>> pairs) {
        log.debug("Prepare ingest for topic {}", topicData.getName());
        if (topicData.getWriteType() == TopicWriteType.MUTABLE) {
            return Single.just(new IngestLists<>(pairs, Collections.emptyList()));
        }
        return this.getExistingKeys(topicData, pairs);
    }

    private <K, V> Single<IngestLists<K, V>> getExistingKeys(final QuickTopicData<K, V> topicData,
        final List<KeyValuePair<K, V>> pairs) {
        final MirrorClient<K, V> client =
            new DefaultMirrorClient<>(topicData.getName(), this.client, this.mirrorConfig,
                    topicData.getValueData().getResolver());

        return Flowable.fromIterable(pairs)
            .map(pair -> {
                    // add info whether the key already exists
                    final boolean keyExists = client.exists(pair.getKey());
                    return IngestPair.from(pair, keyExists);
                }
            )
            .toList()
            .map(IngestLists::fromPairs);
    }

    private enum IngestType {
        NOT_EXISTING,
        EXISTING;

        public static IngestType forPair(final IngestPair<?, ?> pair) {
            return pair.exists() ? EXISTING : NOT_EXISTING;
        }
    }

    /**
     * Pojo holding key value pairs that already exist and those that still have to be ingested.
     */
    @Value
    public static class IngestLists<K, V> {
        List<KeyValuePair<K, V>> dataToIngest;
        List<KeyValuePair<K, V>> existingData;

        private static <K, V> IngestLists<K, V> fromPairs(final Collection<IngestPair<K, V>> pairs) {
            final Map<IngestType, List<KeyValuePair<K, V>>> partitions = pairs.stream()
                .collect(Collectors.groupingBy(
                    IngestType::forPair,
                    Collectors.mapping(IngestPair::getPair, Collectors.toList())
                ));

            final List<KeyValuePair<K, V>> existing =
                partitions.getOrDefault(IngestType.EXISTING, Collections.emptyList());
            final List<KeyValuePair<K, V>> toIngest =
                partitions.getOrDefault(IngestType.NOT_EXISTING, Collections.emptyList());

            return new IngestLists<>(toIngest, existing);
        }
    }

    @Value
    private static class IngestPair<K, V> {
        KeyValuePair<K, V> pair;
        IngestType ingestType;

        private static <K, V> IngestPair<K, V> from(final KeyValuePair<K, V> pair, final boolean exists) {
            final IngestType type = exists ? IngestType.EXISTING : IngestType.NOT_EXISTING;
            return new IngestPair<>(pair, type);
        }

        private boolean exists() {
            return this.ingestType == IngestType.EXISTING;
        }
    }
}
