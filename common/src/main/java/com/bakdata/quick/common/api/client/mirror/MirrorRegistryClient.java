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

package com.bakdata.quick.common.api.client.mirror;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.ingest.IngestClient;
import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.config.TopicRegistryConfig;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.type.QuickTopicType;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import jakarta.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * A client for interacting backed by a topic.
 *
 * <p>
 * Since it is based on a topic, the register method calls the ingest service to put the data into the topic. Getting
 * the topic data requires an existing mirror application.
 */
@Singleton
@Slf4j
public class MirrorRegistryClient implements TopicRegistryClient {
    private final String registryTopic;
    private final IngestClient ingestClient;
    private final MirrorClient<String, TopicData> topicDataClient;
    private final TopicData registryData;

    /**
     * Injectable constructor.
     *
     * @param topicRegistryConfig configuration for Quick's topic registry
     * @param ingestClient http client for ingest service
     * @param client http client
     */
    public MirrorRegistryClient(final TopicRegistryConfig topicRegistryConfig, final IngestClient ingestClient,
        final HttpClient client) {
        this.registryTopic = topicRegistryConfig.getTopicName();
        this.ingestClient = ingestClient;
        this.topicDataClient = createMirrorClient(topicRegistryConfig, client);
        this.registryData = new TopicData(this.registryTopic, TopicWriteType.MUTABLE, QuickTopicType.STRING,
            QuickTopicType.AVRO, null);
    }

    @Override
    public Completable register(final String name, final TopicData data) {
        log.debug("Register topic {}", name);
        return this.ingestClient.sendData(this.registryTopic, List.of(new KeyValuePair<>(data.getName(), data)));
    }

    @Override
    public Completable delete(final String name) {
        log.debug("Delete topic {} in topic registry", name);
        return this.ingestClient.deleteData(this.registryTopic, List.of(name));
    }

    @Override
    public Single<TopicData> getTopicData(final String name) {
        if (name.equals(this.registryTopic)) {
            return this.getSelf();
        }
        return Single.fromCallable(() -> this.fetchTopicData(name));
    }

    private TopicData fetchTopicData(final String name) {
        log.debug("Request topic data from topic registry for topic {}", name);
        final TopicData topicData = this.topicDataClient.fetchValue(name);
        if (topicData != null) {
            return topicData;
        }
        throw new NotFoundException(String.format("Topic %s not found in topic registry", name));
    }

    @Override
    public Single<Boolean> topicDataExists(final String name) {
        return Single.fromCallable(() -> this.topicDataClient.exists(name));
    }

    @Override
    public Single<List<TopicData>> getAllTopics() {
        return Flowable.defer(() -> Flowable.fromIterable(this.topicDataClient.fetchAll()))
            .toSortedList(Comparator.comparing(TopicData::getName));
    }

    private static MirrorClient<String, TopicData> createMirrorClient(final TopicRegistryConfig topicRegistryConfig,
        final HttpClient client) {
        final KnownTypeResolver<TopicData> typeResolver =
            new KnownTypeResolver<>(TopicData.class, client.objectMapper());
        final MirrorHost mirrorHost = new MirrorHost(topicRegistryConfig.getServiceName(), MirrorConfig.directAccess());
        return new DefaultMirrorClient<>(mirrorHost, client, typeResolver, new DefaultMirrorRequestManager(client));
    }

    private Single<TopicData> getSelf() {
        return Single.just(this.registryData);
    }
}
