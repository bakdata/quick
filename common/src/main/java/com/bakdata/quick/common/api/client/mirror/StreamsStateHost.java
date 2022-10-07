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

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;

/**
 * Provides information about the host that enables access to the endpoint that delivers info about Kafka Streams app
 * state.
 */
@Slf4j
public final class StreamsStateHost {
    private final HttpUrl url;

    /**
     * Private to creates the host with the topic name and mirror config. The host can be a service name or an IP.
     *
     * @param url The URL to connect to the Mirror
     */
    private StreamsStateHost(final HttpUrl url) {
        this.url = url;
    }

    /**
     * Static factory method that constructs StreamsStateHost from an instance of the MirrorHost.
     *
     * @param mirrorHost mirror host
     * @return an instance of StreamsStateHost
     */
    public static StreamsStateHost createFromMirrorHost(final MirrorHost mirrorHost) {
        final String mirrorHostUrl = mirrorHost.getUrl().toString();
        final HttpUrl url = HttpUrl.parse(mirrorHostUrl);
        return new StreamsStateHost(Objects.requireNonNull(url, "Invalid mirror host URL"));
    }

    /**
     * Generates a URL for fetching partition info.
     */
    public HttpUrl getPartitionToHostUrl() {
        log.debug("Preparing StreamStateHost URL: {}", this.url);
        final HttpUrl.Builder builder = Objects.requireNonNull(this.url, "The url is not valid").newBuilder();
        return builder.addPathSegment("streams")
            .addPathSegment("partitions").build();
    }

    @Override
    public String toString() {
        return this.url.toString();
    }
}
