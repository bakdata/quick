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

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;

/**
 * Provides information about the host that enables access to the endpoint that delivers info about Kafka Streams app
 * state.
 */
@Slf4j
public final class StreamsStateHost {
    private static final String DEFAULT_MIRROR_SCHEME = "http";
    private final String host;

    /**
     * Private constructor for creating StreamsStateHost.
     *
     * @param topic the host of the mirror. This can be a service name or an IP.
     * @param config mirror config to use. This can set the service prefix and REST path.
     */
    private StreamsStateHost(final String topic, final MirrorConfig config) {
        this.host = config.getPrefix() + topic;
    }

    /**
     * Static factory method that constructs StreamsStateHost from an instance of the MirrorHost.
     *
     * @param mirrorHost mirror host
     * @return an instance of StreamsStateHost
     */
    public static StreamsStateHost fromMirrorHost(final MirrorHost mirrorHost) {
        final String topic = mirrorHost.getTopic();
        final MirrorConfig mirrorConfig = mirrorHost.getConfig();
        return new StreamsStateHost(topic, mirrorConfig);
    }

    /**
     * Generates a URL for fetching partition info.
     */
    public HttpUrl getPartitionToHostUrl() {
        final HttpUrl httpUrl = new Builder()
            .scheme(DEFAULT_MIRROR_SCHEME)
            .host(this.host)
            .addPathSegment("streams")
            .addPathSegment("partitions")
            .build();

        log.debug("Preparing StreamStateHost URL: {}", httpUrl);
        return httpUrl;
    }

    @Override
    public String toString() {
        return String.format("http://%s/", this.host);
    }
}
