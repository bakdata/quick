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

package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.config.TopicRegistryConfig;
import com.bakdata.quick.common.resolver.KnownTypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;

import javax.inject.Singleton;
import java.util.Comparator;
import java.util.List;

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
     * @param ingestClient        http client for ingest service
     * @param client              http client
     * @param topicTypeService    topic type service for config info
     */
    public MirrorRegistryClient(final TopicRegistryConfig topicRegistryConfig, final IngestClient ingestClient,
        final HttpClient client, final TopicTypeService topicTypeService) {
        this.registryTopic = topicRegistryConfig.getTopicName();
        this.ingestClient = ingestClient;
        this.registryData = new TopicData(this.registryTopic, TopicWriteType.MUTABLE, QuickTopicType.STRING,
                QuickTopicType.SCHEMA, null);
        this.topicDataClient = createMirrorClient(topicRegistryConfig, client, registryTopic, topicTypeService);


    }

    @Override
    public Completable register(final String name, final TopicData data) {
        log.debug("Register topic {}", name);
        return this.ingestClient.sendData(this.registryTopic, List.of(new KeyValuePair<>(data.getName(), data)));
    }

    @Override
    public Completable delete(final String name) {
        log.debug("Delete topic {} in registry", name);
        return this.ingestClient.deleteData(this.registryTopic, List.of(name));
    }

    @Override
    public Single<TopicData> getTopicData(final String name) {
        if (name.equals(this.registryTopic)) {
            return this.getSelf();
        }
        log.debug("Request topic data for topic {}", name);
        return Single.fromCallable(() -> this.topicDataClient.fetchValue(name));
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
        final HttpClient client, final String topic, final TopicTypeService topicTypeService) {
        final Single<QuickTopicData<String, TopicData>> temp = topicTypeService.getTopicData(topic);
        final Serde<String> keySerde = temp.blockingGet().getKeyData().getSerde();
        final KnownTypeResolver<TopicData> typeResolver =
            new KnownTypeResolver<>(TopicData.class, client.objectMapper());
        final String serviceName = topicRegistryConfig.getServiceName();
        return new DefaultMirrorClient<>(serviceName, client, MirrorConfig.directAccess(), keySerde, typeResolver);
    }

    private Single<TopicData> getSelf() {
        return Single.just(this.registryData);
    }
}
