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

package com.bakdata.quick.common;

import com.bakdata.quick.common.api.client.MirrorRegistryClient;
import com.bakdata.quick.common.api.client.TopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock for the topic registry client.
 */
@Singleton
@Requires(env = Environment.TEST)
@Replaces(MirrorRegistryClient.class)
public class TestTopicRegistryClient implements TopicRegistryClient {
    private final Map<String, TopicData> store;

    public TestTopicRegistryClient() {
        this.store = new HashMap<>();
    }

    @Override
    public Completable register(final String name, final TopicData data) {
        this.store.put(name, data);
        return Completable.complete();
    }

    @Override
    public Completable delete(final String name) {
        this.store.remove(name);
        return Completable.complete();
    }

    @Override
    public Single<TopicData> getTopicData(final String name) {
        return Single.just(this.store.get(name));
    }

    @Override
    public Single<Boolean> topicDataExists(final String name) {
        return Single.just(this.store.containsKey(name));
    }

    @Override
    public Single<List<TopicData>> getAllTopics() {
        return Single.just(new ArrayList<>(this.store.values()));
    }
}
