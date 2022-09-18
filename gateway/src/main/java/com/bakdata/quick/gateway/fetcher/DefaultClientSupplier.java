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
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.util.Lazy;
import io.reactivex.Single;

final class DefaultClientSupplier implements ClientSupplier {
    private final HttpClient client;
    private final TopicTypeService topicTypeService;
    private final MirrorConfig mirrorConfig;

    DefaultClientSupplier(final HttpClient client, final TopicTypeService topicRegistryClient,
                          final MirrorConfig mirrorConfig) {
        this.client = client;
        this.topicTypeService = topicRegistryClient;
        this.mirrorConfig = mirrorConfig;
    }

    @Override
    public <T> DataFetcherClient<T> createClient(final String topic) {
        return this.doCreateClient(topic);
    }

    private <T> DataFetcherClient<T> doCreateClient(final String topic) {
        final Lazy<TypeResolver<T>> quickTopicTypeLazy = new Lazy<>(() -> this.getQuickTopicTypeLazy(topic));
        return new MirrorDataFetcherClient<>(
            topic, this.client,
            this.mirrorConfig,
            quickTopicTypeLazy
        );
    }

    private <T> TypeResolver<T> getQuickTopicTypeLazy(final String topic) {
        final Single<QuickTopicData<Object, T>> data = this.topicTypeService.getTopicData(topic);
        final QuickTopicData<?, T> topicData = data.blockingGet();
        if (topicData == null) {
            throw new NotFoundException("Could not find topic " + topic);
        }
        return topicData.getValueData().getResolver();
    }
}
