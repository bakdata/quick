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

import com.bakdata.quick.common.api.client.DefaultMirrorClient;
import com.bakdata.quick.common.api.client.DefaultMirrorRequestManager;
import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.client.MirrorClient;
import com.bakdata.quick.common.config.MirrorConfig;
import com.bakdata.quick.common.resolver.TypeResolver;
import com.bakdata.quick.common.util.Lazy;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;


/**
 * A HTTP client for fetching values from mirror REST APIs.
 */
public class MirrorDataFetcherClient<V> implements DataFetcherClient<V> {
    private final Lazy<MirrorClient<String, V>> mirrorClient;

    /**
     * Constructor for client.
     *
     * @param host             host url of the mirror
     * @param client           http client
     * @param mirrorConfig     configuration for the mirror
     * @param typeResolverLazy a lazy for the value resolver
     */
    public MirrorDataFetcherClient(final String host, final HttpClient client, final MirrorConfig mirrorConfig,
                                   final Lazy<TypeResolver<V>> typeResolverLazy) {
        this.mirrorClient =
            new Lazy<>(() -> this.createMirrorClient(host, mirrorConfig, client, typeResolverLazy.get()));
    }

    public MirrorDataFetcherClient(final String host, final HttpClient client, final MirrorConfig mirrorConfig,
                                   final TypeResolver<V> valueResolver) {
        this(host, client, mirrorConfig, new Lazy<>(() -> valueResolver));
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

    private DefaultMirrorClient<String, V> createMirrorClient(final String host, final MirrorConfig mirrorConfig,
                                                              final HttpClient client,
                                                              final TypeResolver<V> valueResolver) {
        return new DefaultMirrorClient<>(host, client, mirrorConfig, valueResolver,
            new DefaultMirrorRequestManager(client));
    }
}
