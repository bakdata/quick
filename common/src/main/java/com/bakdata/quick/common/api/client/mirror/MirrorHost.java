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

import com.bakdata.quick.common.config.MirrorConfig;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

/**
 * Utility for setting a Mirror host in Quick.
 */
@Getter
@Slf4j
public class MirrorHost {
    private static final String DEFAULT_MIRROR_HOST_PATH = "mirror";
    private static final String DEFAULT_MIRROR_SCHEME = "http";
    private final String topic;
    private final MirrorConfig config;
    private final String host;
    private final HttpUrl url;

    /**
     * Private to creates the host with the topic name and mirror config. The host can be a service name or an IP.
     *
     * @param topic the topic of the mirror.
     * @param config mirror config to use. This can set the service prefix and REST path.
     */
    public MirrorHost(final String topic, final MirrorConfig config) {
        this.config = config;
        this.topic = topic;
        this.host = this.config.getPrefix() + this.topic;
        final String stringUrl = String.format("%s://%s", DEFAULT_MIRROR_SCHEME, this.host);
        this.url = HttpUrl.parse(stringUrl);
    }

    /**
     * Generates a URL for fetching a single key in a topic.
     *
     * <p>
     * e.g. http://quick-mirror-example-topic/mirror/123
     */
    public HttpUrl forKey(final String key) {
        final HttpUrl httpUrl = this.getBaseUrlBuilder()
            .addPathSegment(key)
            .build();

        log.trace("Preparing Mirror URL: {}", httpUrl);
        return httpUrl;
    }

    /**
     * Generates a URL for fetching a list of keys in a topic.
     *
     * <p>
     * e.g. http://quick-mirror-example-topic/mirror/keys?ids=123,456
     */
    public HttpUrl forKeys(final Iterable<String> keys) {
        final String ids = String.join(",", keys);

        final HttpUrl httpUrl = this.getBaseUrlBuilder()
            .addPathSegment("keys")
            .addEncodedQueryParameter("ids", ids).build();

        log.trace("Preparing Mirror URL: {}", httpUrl);
        return httpUrl;
    }

    /**
     * Generates a URL for fetching all keys in a topic.
     *
     * <p>
     * e.g. http://quick-mirror-example-topic/mirror/
     */
    public HttpUrl forAll() {
        final HttpUrl httpUrl = this.getBaseUrlBuilder().build();

        log.trace("Preparing Mirror URL: {}", httpUrl);
        return httpUrl;
    }

    /**
     * Generates a URL for fetching a range of keys.
     *
     * <p>
     * e.g. http://quick-mirror-example-topic/mirror/range/123?from=1&to=10
     */
    public HttpUrl forRange(final String key, final String from, final String to) {
        final HttpUrl httpUrl = this.getBaseUrlBuilder()
            .addPathSegment("range")
            .addPathSegment(key)
            .addQueryParameter("from", from)
            .addQueryParameter("to", to)
            .build();

        log.trace("Preparing Mirror URL: {}", httpUrl);
        return httpUrl;
    }

    /**
     * Returns the Mirror host with the configured prefix.
     *
     * <p>
     * e.g. http://quick-mirror-host-name/mirror
     */
    @Override
    public String toString() {
        return this.getBaseUrlBuilder().toString();
    }

    /**
     * If the host (name) of the two mirrors is the same they are equal.
     */
    @Override
    public boolean equals(final Object otherMirrorHost) {
        if (this == otherMirrorHost) {
            return true;
        }
        if (!(otherMirrorHost instanceof MirrorHost)) {
            return false;
        }
        final MirrorHost that = (MirrorHost) otherMirrorHost;
        return Objects.equals(this.topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.topic);
    }

    private HttpUrl.Builder getBaseUrlBuilder() {
        return Objects.requireNonNull(this.url, "The url is not valid")
            .newBuilder()
            .addPathSegment(DEFAULT_MIRROR_HOST_PATH);
    }
}
