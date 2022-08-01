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

package com.bakdata.quick.mirror.base;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.apache.kafka.streams.state.HostInfo;

/**
 * Config for pods containing information about this host address.
 */
@Singleton
@Getter
public class HostConfig {
    private final String host;
    private final int port;

    public HostConfig(@Property(name = "pod.ip") final String host, final EmbeddedServer server) {
        this.host = host;
        this.port = server.getPort();
    }

    public HostInfo toInfo() {
        return new HostInfo(this.host, this.port);
    }

    public String toConnectionString() {
        return String.format("%s:%s", this.getHost(), this.getPort());
    }
}
