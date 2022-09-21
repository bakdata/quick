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
    public <K, V> DataFetcherClient<K, V> createClient(final String topic) {
        return this.doCreateClient(topic);
    }

    private <K, V> DataFetcherClient<K, V> doCreateClient(final String topic) {
        return new MirrorDataFetcherClient<>(
            topic,
            this.client,
            this.mirrorConfig,
            this.topicTypeService
        );
    }
}
