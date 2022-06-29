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
import com.bakdata.quick.common.api.client.BaseMirrorClient;
import com.bakdata.quick.common.api.client.MirrorClient;
import com.bakdata.quick.common.api.client.PartitionedMirrorClient;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.util.KeySerdeValResolverWrapper;
import com.bakdata.quick.common.util.Lazy;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.kafka.common.serialization.Serde;

import java.util.List;


/**
 * A HTTP client for fetching values from mirror REST APIs.
 */
public class MirrorDataFetcherClient<V> implements DataFetcherClient<V> {
    private final Lazy<MirrorClient<String, V>> mirrorClient;

    /**
     * Constructor for client.
     *
     * @param host   host url of the mirror
     * @param client http client
     * @param mirrorConfig configuration for the mirror
     * @param wrapper a wrapper of the key serde and value resolver
     */
    public MirrorDataFetcherClient(final String host, final HttpClient client,
                                   final MirrorConfig mirrorConfig,
                                   final Lazy<KeySerdeValResolverWrapper<String, V>> wrapper) {
        this.mirrorClient = new Lazy<>(() -> this.createMirrorClient(host, mirrorConfig, client,
                wrapper.get().getKeySerde(), wrapper.get().getValueTypeResolver()));
    }

    public MirrorDataFetcherClient(final String host, final HttpClient client,
                                   final MirrorConfig mirrorConfig,
                                   final KeySerdeValResolverWrapper<String, V> wrapper) {
        this(host, client, mirrorConfig, new Lazy<>(() -> wrapper));
    }

    @Override
    @Nullable
    public V fetchResult(final String id) {
        return this.mirrorClient.get().fetchValue(id);
    }

    @Override
    @Nullable
    public List<V> fetchResults(final List<String> ids) {
        return this.mirrorClient.get().fetchValues(ids);
    }

    @Override
    @Nullable
    public List<V> fetchList() {
        return this.mirrorClient.get().fetchAll();
    }

    private BaseMirrorClient<String, V> createMirrorClient(final String host, final MirrorConfig mirrorConfig,
                                                           final HttpClient client, Serde<String> keySerde, final TypeResolver<V> valueResolver) {
        return new PartitionedMirrorClient<>(host, client, mirrorConfig, keySerde, valueResolver);
    }
}
