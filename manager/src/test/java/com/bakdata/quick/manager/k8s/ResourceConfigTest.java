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

package com.bakdata.quick.manager.k8s;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@MicronautTest(environments = Environment.KUBERNETES)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Property(name = "quick.manager.update-managed-images", value = "false")
@Property(name= "quick.manager.create-topic-registry", value = "false")
class ResourceConfigTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return Map.of(
            "quick.applications.resources.memory.limit", "200Mi",
            "quick.applications.resources.memory.request", "150Mi",
            "quick.applications.resources.cpu.limit", "5",
            "quick.applications.resources.cpu.request", "1"
        );
    }

    @Test
    void shouldSetMemoryLimit(final ResourceConfig resourceConfig) {
        assertThat(resourceConfig.getMemory().getLimit()).isEqualTo("200Mi");
    }

    @Test
    void shouldSetMemoryRequest(final ResourceConfig resourceConfig) {
        assertThat(resourceConfig.getMemory().getRequest()).isEqualTo("150Mi");
    }

    @Test
    void shouldSetCpuLimit(final ResourceConfig resourceConfig) {
        assertThat(resourceConfig.getCpu().getLimit()).isEqualTo("5");
    }

    @Test
    void shouldSetCpuRequest(final ResourceConfig resourceConfig) {
        assertThat(resourceConfig.getCpu().getRequest()).isEqualTo("1");
    }
}
