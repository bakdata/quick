package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.config.MirrorConfig;

public class StreamsStateHost {

    private final String host;
    private final MirrorConfig config;

    /**
     * Default constructor.
     *
     * @param host the host of the mirror. This can be a service name or an IP.
     * @param config mirror config to use. This can set the service prefix and REST path.
     */
    private StreamsStateHost(final String host, final MirrorConfig config) {
        this.host = host;
        this.config = config;
    }

    public static StreamsStateHost fromMirrorHost(MirrorHost mirrorHost) {
        String host = mirrorHost.getHost();
        MirrorConfig mirrorConfig = MirrorConfig.getConfigForPartitionMappingInfo();
        return new StreamsStateHost(host, mirrorConfig);
    }

    /**
     * Generates a URL for fetching partition info.
     */
    public String getPartitionToHostUrl() {
        return String.format("http://%s%s", this.host, this.config.getPath());
    }
}
