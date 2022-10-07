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
import java.util.Optional;
import lombok.Getter;

/**
 * Configuration for setting common mirror attributes based on deployment.
 */
@ConfigurationProperties("quick.mirror")
@Getter
public class MirrorConfig {

    public static final String DEFAULT_MIRROR_HOST_PREFIX = "quick-mirror-";
    public static final String DEFAULT_MIRROR_HOST_PATH = "mirror";
    public static final String DEFAULT_STREAMS_STATE_PATH = "streams";
    public static final int DEFAULT_MIRROR_POD_PORT = 8080;

    private final String prefix;

    /**
     * Constructor setting defaults.
     */
    public MirrorConfig() {
        this.prefix = DEFAULT_MIRROR_HOST_PREFIX;
    }

    /**
     * Injectable constructor from properties.
     *
     * <p>
     * The parameters are optional. If not set, their defaults are used. Defaults to
     * {@link MirrorConfig#DEFAULT_MIRROR_HOST_PREFIX} for empty optionals.
     *
     * <p>
     * e.g. if "quick-mirror" is the prefix and the topic name is "test-topic" the mirror URL is
     * http://quick-mirror-test-topic/.
     *
     * @param prefix prefix of mirror
     */
    @ConfigurationInject
    public MirrorConfig(final Optional<String> prefix) {
        this.prefix = prefix.orElse(DEFAULT_MIRROR_HOST_PREFIX);
    }
}
