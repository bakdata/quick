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
public class MirrorDataFetcherClient<T> implements DataFetcherClient<T> {
    private final Lazy<MirrorClient<String, T>> mirrorClient;

    /**
     * Constructor for client.
     *
     * @param host   host url of the mirror
     * @param client http client
     * @param type   key type
     */
    public MirrorDataFetcherClient(final String host, final HttpClient client, final MirrorConfig mirrorConfig,
        final Lazy<TypeResolver<T>> type) {
        this.mirrorClient = new Lazy<>(() -> this.createMirrorClient(host, mirrorConfig, client, type));
    }

    public MirrorDataFetcherClient(final String host, final HttpClient client, final MirrorConfig mirrorConfig,
        final TypeResolver<T> type) {
        this(host, client, mirrorConfig, new Lazy<>(() -> type));
    }

    @Override
    @Nullable
    public T fetchResult(final String id) {
        return this.mirrorClient.get().fetchValue(id);
    }

    @Override
    @Nullable
    public List<T> fetchResults(final List<String> ids) {
        return this.mirrorClient.get().fetchValues(ids);
    }

    @Override
    @Nullable
    public List<T> fetchList() {
        return this.mirrorClient.get().fetchAll();
    }

    private DefaultMirrorClient<String, T> createMirrorClient(final String host, final MirrorConfig mirrorConfig,
        final HttpClient client, final Lazy<TypeResolver<T>> resolver) {
        return new DefaultMirrorClient<>(host, client, mirrorConfig, resolver.get());
    }
}
