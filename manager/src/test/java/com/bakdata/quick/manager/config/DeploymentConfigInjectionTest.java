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

package com.bakdata.quick.manager.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeploymentConfigInjectionTest implements TestPropertyProvider {
    @Inject
    private DeploymentConfig deploymentConfig;

    @NotNull
    @Override
    public Map<String, String> getProperties() {
        return Map.ofEntries(
            Map.entry("quick.default-image-tag", "0.1.2"),
            Map.entry("quick.docker-registry", "hub.docker.com"),
            Map.entry("quick.default-replicas", "3"),
            Map.entry("quick.ingress-host", "test"),
            Map.entry("quick.ingress-ssl", "true"),
            Map.entry("quick.ingress-entrypoint", "web")
        );
    }

    @Test
    void shouldInjectConfig() {
        assertThat(this.deploymentConfig.getDefaultImageTag()).isEqualTo("0.1.2");
        assertThat(this.deploymentConfig.getDefaultReplicas()).isEqualTo(3);
        assertThat(this.deploymentConfig.getDockerRegistry()).isEqualTo("hub.docker.com");
        assertThat(this.deploymentConfig.getIngressHost()).isEqualTo(Optional.of("test"));
        assertThat(this.deploymentConfig.isIngressSsl()).isTrue();
        assertThat(this.deploymentConfig.getIngressEntrypoint()).isEqualTo("web");
    }
}
