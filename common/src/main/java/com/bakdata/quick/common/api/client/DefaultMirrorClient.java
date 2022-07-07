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

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import org.jetbrains.annotations.Nullable;
import okhttp3.ResponseBody;
import java.util.List;

/**
 * Default HTTP client for working with Quick mirrors.
 *
 * @param <K> key type
 * @param <V> value type
 */

public class DefaultMirrorClient<K, V> implements MirrorClient<K, V> {

    private final MirrorClient<K, V> delegate;

    /**
     * Constructor for the client.
     *
     * @param topicName    name of the topic the mirror is deployed
     * @param client       http client
     * @param mirrorConfig configuration of the mirror host
     * @param valueResolver the value's {@link TypeResolver}
     */
    public DefaultMirrorClient(final String topicName, final HttpClient client, final MirrorConfig mirrorConfig,
                               final TypeResolver<V> valueResolver) {
        this(new MirrorHost(topicName, mirrorConfig), client, valueResolver);
    }

    /**
     * Constructor that can be used when the mirror client is based on an IP or other non-standard host.
     *
     * @param mirrorHost   host to use
     * @param client       http client
     * @param valueResolver the value's {@link TypeResolver}
     */
    public DefaultMirrorClient(final MirrorHost mirrorHost, final HttpClient client,
                               final TypeResolver<V> valueResolver) {
        this.delegate = new BaseMirrorClient<>(mirrorHost, client, valueResolver);
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        return this.delegate.fetchValue(key);
    }

    @Override
    public List<V> fetchAll() {
        return this.delegate.fetchAll();
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        return this.delegate.fetchValues(keys);
    }

    @Override
    public boolean exists(final K key) {
        return this.delegate.exists(key);
    }

    @Nullable
    @Override
    public <T> T sendRequest(final String url, final ParserFunction<T> parser) {
        return this.delegate.sendRequest(url, parser);
    }

    @Nullable
    @Override
    public ResponseBody makeRequest(final String url) {
        return this.delegate.makeRequest(url);
    }
}
