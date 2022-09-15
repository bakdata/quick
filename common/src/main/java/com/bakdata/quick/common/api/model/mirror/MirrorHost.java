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

package com.bakdata.quick.common.api.model.mirror;

import com.bakdata.quick.common.config.MirrorConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for setting a Mirror host in Quick.
 */
@Getter
@Slf4j
public class MirrorHost {
    private final String host;
    private final MirrorConfig config;

    /**
     * Default constructor.
     *
     * @param host the host of the mirror. This can be a service name or an IP.
     * @param config mirror config to use. This can set the service prefix and REST path.
     */
    public MirrorHost(final String host, final MirrorConfig config) {
        this.host = host;
        this.config = config;
    }

    /**
     * Generates a URL for fetching a single key in a topic.
     */
    public String forKey(final String key) {
        final String url =
            String.format("http://%s%s/%s/%s", this.config.getPrefix(), this.host, this.config.getPath(), key);
        log.trace("Preparing Mirror URL: {}", url);
        return url;
    }

    /**
     * Generates a URL for fetching a list of keys in a topic.
     */
    public String forKeys(final Iterable<String> keys) {
        final String ids = String.join(",", keys);
        final String url =
            String.format("http://%s%s/%s/keys?ids=%s", this.config.getPrefix(), this.host, this.config.getPath(), ids);
        log.trace("Preparing Mirror URL: {}", url);
        return url;
    }

    /**
     * Generates a URL for fetching all keys in a topic.
     */
    public String forAll() {
        final String url = String.format("http://%s%s/%s", this.config.getPrefix(), this.host, this.config.getPath());
        log.trace("Preparing Mirror URL: {}", url);
        return url;
    }

    /**
     * Generates a URL for fetching a range of keys.
     */
    public String forRange(final String key, final String from, final String to) {
        return String.format("http://%s%s/%s/range/%s?from=%s&to=%s", this.config.getPrefix(), this.host,
            this.config.getPath(), key, from, to);
    }

    /**
     * Generates a URL without any keys.
     */
    public String plainUrl() {
        return String.format("http://%s%s/", this.config.getPrefix(), this.host);
    }
}
