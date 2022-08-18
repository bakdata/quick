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

package com.bakdata.quick.manager.k8s.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import com.bakdata.quick.manager.config.ApplicationSpecificationConfig;
import com.bakdata.quick.manager.config.ImagePullPolicy;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;


class ApplicationSpecificationConfigTest {
    @Test
    void shouldCreateConfigWithDefault() {
        final ApplicationSpecificationConfig
            specConfig = ConfigUtils.createWithProperties(Collections.emptyMap(), ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy()).isEqualTo(ImagePullPolicy.ALWAYS);
    }

    @Test
    void shouldCreateConfigWithImagePullPolicyIfNotPresent() {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.imagePullPolicy", ImagePullPolicy.IF_NOT_PRESENT
        );
        final ApplicationSpecificationConfig specConfig =
            ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy()).isEqualTo(ImagePullPolicy.IF_NOT_PRESENT);
    }

    @Test
    void shouldCreateConfigWithImagePullPolicyNever() {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.imagePullPolicy", ImagePullPolicy.NEVER
        );
        final ApplicationSpecificationConfig
            specConfig = ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy()).isEqualTo(ImagePullPolicy.NEVER);
    }

    @Test
    void shouldCreateConfigHardwareResources() {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.resources.memory.limit", "200Mi",
            "quick.applications.spec.resources.memory.request", "150Mi",
            "quick.applications.spec.resources.cpu.limit", "5",
            "quick.applications.spec.resources.cpu.request", "1"
        );
        final ApplicationSpecificationConfig
            specConfig = ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy()).isEqualTo(ImagePullPolicy.ALWAYS);

        assertThat(specConfig.getResources().getMemory().getLimit()).isEqualTo("200Mi");
        assertThat(specConfig.getResources().getMemory().getRequest()).isEqualTo("150Mi");
        assertThat(specConfig.getResources().getCpu().getLimit()).isEqualTo("5");
        assertThat(specConfig.getResources().getCpu().getRequest()).isEqualTo("1");
    }
}
