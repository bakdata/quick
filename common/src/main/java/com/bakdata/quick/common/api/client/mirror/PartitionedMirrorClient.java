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
import com.bakdata.quick.common.api.client.routing.Router;
import com.bakdata.quick.common.resolver.TypeResolver;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

/**
 * MirrorClient that has access to information about partition-host mapping. This information enables it to efficiently
 * route requests in the case when there is more than one mirror replica.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Slf4j
public class PartitionedMirrorClient<K, V> implements MirrorClient<K, V> {
    private final Router<K> router;
    private final MirrorValueParser<V> parser;
    private final MirrorRequestManager requestManager;

    /**
     * Next to its default task of instantiation PartitionHost, it takes responsibility for creating several business
     * objects and initializing the PartitionRouter with a mapping retrieved from StreamController.
     */
    public PartitionedMirrorClient(final HttpClient client,
        final TypeResolver<V> typeResolver,
        final MirrorRequestManager mirrorRequestManager,
        final Router<K> router) {
        this.parser = new MirrorValueParser<>(typeResolver, client.objectMapper());
        this.requestManager = mirrorRequestManager;
        this.router = router;
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        final MirrorHost currentKeyHost = this.router.findHost(key);
        final ResponseWrapper response = this.requestManager.makeRequest(currentKeyHost.forKey(key.toString()));
        if (response.isUpdateCacheHeaderSet()) {
            this.router.updateRoutingInfo();
        }
        return this.requestManager.processResponse(response, this.parser::deserialize);
    }

    @Override
    public List<V> fetchAll() {
        final List<MirrorHost> knownHosts = this.router.getAllHosts();
        final List<V> valuesFromAllHosts = new ArrayList<>();
        for (final MirrorHost host : knownHosts) {
            final ResponseWrapper response = this.requestManager.makeRequest(host.forAll());
            final List<V> valuesFromSingleHost =
                Objects.requireNonNullElse(this.requestManager.processResponse(response, this.parser::deserializeList),
                    Collections.emptyList());
            valuesFromAllHosts.addAll(valuesFromSingleHost);
            log.trace("Fetched {} values.", valuesFromSingleHost.size());
        }
        return valuesFromAllHosts;
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        log.trace("Fetching values for keys {}.", keys.size());
        final List<V> valuesFromAllHosts = new ArrayList<>();

        final Map<MirrorHost, List<K>> mirrorHostKeyMap = this.findMirrorHostForListOfKeys(keys);
        log.trace("Created a map of host and list of keys: {}", mirrorHostKeyMap);

        for (final Entry<MirrorHost, List<K>> mirrorHostWitKeys : mirrorHostKeyMap.entrySet()) {
            final List<String> stringKeys = mirrorHostWitKeys.getValue().stream()
                .map(Objects::toString)
                .collect(Collectors.toList());
            final HttpUrl url = mirrorHostWitKeys.getKey().forKeys(stringKeys);
            final ResponseWrapper response = this.requestManager.makeRequest(url);

            if (response.isUpdateCacheHeaderSet()) {
                this.router.updateRoutingInfo();
            }

            final List<V> valuesFromSingleHost =
                Objects.requireNonNullElse(this.requestManager.processResponse(response, this.parser::deserializeList),
                    Collections.emptyList());
            valuesFromAllHosts.addAll(valuesFromSingleHost);
        }
        log.trace("Fetched values for list request: {}", valuesFromAllHosts);
        return valuesFromAllHosts;
    }

    @Override
    @Nullable
    public List<V> fetchRange(final K key, final String from, final String to) {
        final MirrorHost currentKeyHost = this.router.findHost(key);
        final HttpUrl url = currentKeyHost.forRange(key.toString(), from, to);
        final ResponseWrapper response = this.requestManager.makeRequest(url);
        if (response.isUpdateCacheHeaderSet()) {
            this.router.updateRoutingInfo();
        }
        return this.requestManager.processResponse(response, this.parser::deserializeList);
    }

    @Override
    public boolean exists(final K key) {
        return this.fetchValue(key) != null;
    }

    private Map<MirrorHost, List<K>> findMirrorHostForListOfKeys(final Iterable<K> keys) {
        final Map<MirrorHost, List<K>> mirrorHostKeyMap = new HashMap<>();
        for (final K key : keys) {
            final MirrorHost mirrorHost = this.router.findHost(key);
            mirrorHostKeyMap.computeIfAbsent(mirrorHost, k -> new ArrayList<>()).add(key);
        }
        return mirrorHostKeyMap;
    }
}
