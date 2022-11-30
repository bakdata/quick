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
import com.bakdata.quick.common.api.client.mirror.MirrorClientFactory;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;
import org.apache.kafka.common.serialization.Serde;

final class DefaultClientSupplier implements ClientSupplier {
    private final HttpClient client;
    private final MirrorClientFactory mirrorClientFactory;

    DefaultClientSupplier(final HttpClient client,
        final MirrorClientFactory mirrorClientFactory) {
        this.client = client;
        this.mirrorClientFactory = mirrorClientFactory;
    }

    @Override
    public <K, V> DataFetcherClient<K, V> createClient(final String topic,
        final Lazy<QuickTopicData<K, V>> quickTopicData) {
        return new MirrorDataFetcherClient<>(new Lazy<>(() ->
            this.mirrorClientFactory.createMirrorClient(this.client, topic, quickTopicData)));
    }

    @Override
    public <K, V> DataFetcherClient<K, V> createClient(final String topic, final Serde<K> keySerde,
        final Lazy<QuickTopicData<Object, V>> quickTopicData) {
        return new MirrorDataFetcherClient<>(new Lazy<>(() ->
            this.mirrorClientFactory.createMirrorClient(this.client, topic, keySerde,
                quickTopicData.get().getValueData().getResolver())));
    }
}
