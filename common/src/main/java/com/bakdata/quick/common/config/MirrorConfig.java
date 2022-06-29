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

package com.bakdata.quick.common.config;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;

import java.util.Optional;

/**
 * Configuration for setting common mirror attributes based on deployment.
 */
@ConfigurationProperties("quick.mirror")
@Getter
public class MirrorConfig {

    public static final String DEFAULT_MIRROR_HOST_PREFIX = "quick-mirror-";
    public static final String DEFAULT_MIRROR_HOST_PATH = "mirror";
    public static final String DEFAULT_STREAMS_STATE_PATH = "streams";
    public static final String DEFAULT_PARTITION_INFO_PATH = "partitions";
    public static final int DEFAULT_MIRROR_POD_PORT = 8080;

    private final String prefix;
    private final String path;

    /**
     * Constructor setting defaults.
     */
    public MirrorConfig() {
        this.prefix = DEFAULT_MIRROR_HOST_PREFIX;
        this.path = DEFAULT_MIRROR_HOST_PATH;
    }

    /**
     * Injectable constructor from properties.
     *
     * <p>
     * The parameters are optional. If not set, their defaults are used.
     *
     * @param prefix prefix of mirror, e.g. "quick-mirror" in http://quick-mirror-topic/. Defaults to {@link
     * MirrorConfig#DEFAULT_MIRROR_HOST_PREFIX} for empty optionals.
     * @param path rest api path, e.g. "mirror" in http://quick-mirror-topic/mirror/key. Defaults to {@link
     * MirrorConfig#DEFAULT_MIRROR_HOST_PATH} for empty optionals.
     */
    @ConfigurationInject
    public MirrorConfig(final Optional<String> prefix, final Optional<String> path) {
        this.prefix = prefix.orElse(DEFAULT_MIRROR_HOST_PREFIX);
        this.path = path.orElse(DEFAULT_MIRROR_HOST_PATH);
    }

    /**
     * Config when accessing a mirror directly, i.e., not through a service but an IP.
     *
     * @return config with empty prefix.
     */
    public static MirrorConfig directAccess() {
        return new MirrorConfig(Optional.of(""), Optional.of(DEFAULT_MIRROR_HOST_PATH));
    }

    public static MirrorConfig directAccessToStreamsState() {
        return new MirrorConfig(Optional.of(""), Optional.of(DEFAULT_STREAMS_STATE_PATH));
    }


}
