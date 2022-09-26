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
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;

final class DefaultClientSupplier<K, V> implements ClientSupplier<K, V> {
    private final HttpClient client;
    private final MirrorConfig mirrorConfig;

    DefaultClientSupplier(final HttpClient client,
        final MirrorConfig mirrorConfig) {
        this.client = client;
        this.mirrorConfig = mirrorConfig;
    }

    @Override
    public DataFetcherClient<K, V> createClient(final String topic, final Lazy<QuickTopicData<K, V>> quickTopicData) {
        return this.doCreateClient(topic, quickTopicData.get());
    }

    private DataFetcherClient<K, V> doCreateClient(final String topic, final QuickTopicData<K, V> quickTopicData) {
        return new MirrorDataFetcherClient<>(
            topic,
            this.client,
            this.mirrorConfig,
            quickTopicData
        );
    }
}
