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

/**
 * Provides information about the host that enables access to the endpoint that delivers info about Kafka Streams app
 * state.
 */
@Slf4j
public final class StreamsStateHost {

    private final String host;
    private final MirrorConfig config;

    /**
     * Private constructor for creating StreamsStateHost.
     *
     * @param host the host of the mirror. This can be a service name or an IP.
     * @param config mirror config to use. This can set the service prefix and REST path.
     */
    private StreamsStateHost(final String host, final MirrorConfig config) {
        this.host = host;
        this.config = config;
    }

    /**
     * Static factory method that constructs StreamsStateHost from an instance of the MirrorHost.
     *
     * @param mirrorHost mirror host
     * @return an instance of StreamsStateHost
     */
    public static StreamsStateHost fromMirrorHost(final MirrorHost mirrorHost) {
        final String host = mirrorHost.getHost();
        final MirrorConfig mirrorConfig = mirrorHost.getConfig();
        return new StreamsStateHost(host, mirrorConfig);
    }

    /**
     * Generates a URL for fetching partition info.
     */
    public String getPartitionToHostUrl() {
        final String url = String.format("http://%s%s/streams/partitions", this.config.getPrefix(), this.host);
        log.debug("Preparing StreamStateHost URL: {}", url);
        return url;
    }

    @Override
    public String toString() {
        return String.format("http://%s%s/", this.config.getPrefix(), this.host);
    }
}
