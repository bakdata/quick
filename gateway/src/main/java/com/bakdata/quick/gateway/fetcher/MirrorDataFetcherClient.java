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

import com.bakdata.quick.common.api.client.mirror.MirrorClient;
import com.bakdata.quick.common.util.Lazy;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;


/**
 * An HTTP client for fetching values from mirror REST APIs.
 */
@Slf4j
public class MirrorDataFetcherClient<K, V> implements DataFetcherClient<K, V> {
    private final Lazy<MirrorClient<K, V>> mirrorClient;

    /**
     * Constructor for client.
     */
    public MirrorDataFetcherClient(final Lazy<MirrorClient<K, V>> mirrorClient) {
        this.mirrorClient = mirrorClient;
    }

    @Override
    @Nullable
    public V fetchResult(final K id) {
        log.trace("Preparing to send request for fetching a key {} to Mirror", id);
        return this.mirrorClient.get().fetchValue(id);
    }

    @Override
    @Nullable
    public List<V> fetchResults(final List<K> ids) {
        log.trace("Preparing to send request for fetching a list of ids {} to Mirror", ids);
        return this.mirrorClient.get().fetchValues(ids);
    }

    @Override
    @Nullable
    public List<V> fetchList() {
        log.trace("Preparing to send request for fetching all keys from the Mirror");
        return this.mirrorClient.get().fetchAll();
    }

    @Override
    @Nullable
    public List<V> fetchRange(final K id, final String from, final String to) {
        return this.mirrorClient.get().fetchRange(id, from, to);
    }
}
