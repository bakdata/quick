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

import com.bakdata.quick.common.resolver.TypeResolver;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default HTTP client for working with Quick mirrors.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class DefaultMirrorClient<K, V> implements MirrorClient<K, V> {

    private final MirrorHost host;
    private final MirrorValueParser<V> parser;
    private final MirrorRequestManager mirrorRequestManager;

    /**
     * Constructor that can be used when the mirror client is based on an IP or other non-standard host.
     *
     * @param mirrorHost host to use
     * @param requestManager a manager for sending requests to the mirror and processing responses
     */
    public DefaultMirrorClient(final MirrorHost mirrorHost, final MirrorValueParser<V> mirrorValueParser,
        final MirrorRequestManager requestManager) {
        this.host = mirrorHost;
        this.parser = mirrorValueParser;
        this.mirrorRequestManager = requestManager;
    }

    @Override
    @Nullable
    public V fetchValue(final K key) {
        final ResponseWrapper response = this.mirrorRequestManager.makeRequest(this.host.forKey(key.toString()));
        return this.mirrorRequestManager.processResponse(response, this.parser::deserialize);
    }

    @Override
    public List<V> fetchAll() {
        final ResponseWrapper response = this.mirrorRequestManager.makeRequest(this.host.forAll());
        return Objects.requireNonNullElse(
            this.mirrorRequestManager.processResponse(response, this.parser::deserializeList),
            Collections.emptyList());
    }

    @Override
    @Nullable
    public List<V> fetchValues(final List<K> keys) {
        final List<String> collect = keys.stream().map(Object::toString).collect(Collectors.toList());
        final ResponseWrapper response = this.mirrorRequestManager.makeRequest(this.host.forKeys(collect));
        return Objects.requireNonNullElse(
            this.mirrorRequestManager.processResponse(response, this.parser::deserializeList),
            Collections.emptyList());
    }

    @Override
    @Nullable
    public List<V> fetchRange(final K key, final String from, final String rangeTo) {
        final ResponseWrapper response = this.mirrorRequestManager.makeRequest(
            this.host.forRange(key.toString(), from, rangeTo)
        );
        return Objects.requireNonNullElse(
            this.mirrorRequestManager.processResponse(response, this.parser::deserializeList),
            Collections.emptyList());
    }

    @Override
    public boolean exists(final K key) {
        return this.fetchValue(key) != null;
    }
}
