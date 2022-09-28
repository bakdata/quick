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
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.util.Lazy;

/**
 * Creates a {@link DefaultMirrorClient}.
 */
public class DefaultMirrorClientFactory implements MirrorClientFactory {
    @Override
    public <K, V> MirrorClient<K, V> createMirrorClient(final HttpClient client, final String topic,
        final MirrorConfig mirrorConfig, final Lazy<QuickTopicData<K, V>> quickTopicData) {
        final MirrorHost mirrorHost = new MirrorHost(topic, mirrorConfig);
        final MirrorRequestManager requestManager = new DefaultMirrorRequestManager(client);
        final TypeResolver<V> valueTypeResolver = quickTopicData.get().getValueData().getResolver();
        return new DefaultMirrorClient<>(mirrorHost, client, valueTypeResolver, requestManager);
    }
}
