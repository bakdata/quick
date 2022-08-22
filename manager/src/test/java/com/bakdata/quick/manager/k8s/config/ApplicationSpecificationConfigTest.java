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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


class ApplicationSpecificationConfigTest {
    @Test
    void shouldCreateConfigWithDefault() {
        final ApplicationSpecificationConfig
            specConfig = ConfigUtils.createWithProperties(Collections.emptyMap(), ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy().getPolicyName()).isEqualTo(ImagePullPolicy.ALWAYS.getPolicyName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"always", "Always", "ALWAYS"})
    void shouldCreateConfigWithImagePullPolicyAlways(final String alwaysPolicy) {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.imagePullPolicy", alwaysPolicy
        );
        final ApplicationSpecificationConfig specConfig =
            ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);

        assertThat(specConfig.getImagePullPolicy().getPolicyName()).isEqualTo(ImagePullPolicy.ALWAYS.getPolicyName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ifNotPresent", "IfNotPresent", "if_not_present", "IF_NOT_PRESENT"})
    void shouldCreateConfigWithImagePullPolicyIfNotPresent(final String ifNotPresentPolicy) {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.imagePullPolicy", ifNotPresentPolicy
        );
        final ApplicationSpecificationConfig specConfig =
            ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);

        assertThat(specConfig.getImagePullPolicy().getPolicyName())
            .isEqualTo(ImagePullPolicy.IF_NOT_PRESENT.getPolicyName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"never", "Never", "NEVER"})
    void shouldCreateConfigWithImagePullPolicyNever(final String neverPolicy) {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.imagePullPolicy", neverPolicy
        );
        final ApplicationSpecificationConfig
            specConfig = ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy().getPolicyName())
            .isEqualTo(ImagePullPolicy.NEVER.getPolicyName());
    }

    @Test
    void shouldCreateConfigHardwareResources() {
        final Map<String, Object> properties = Map.of(
            "quick.applications.spec.resources.memory.limit", "200Mi",
            "quick.applications.spec.resources.memory.request", "150Mi",
            "quick.applications.spec.resources.cpu.limit", "5",
            "quick.applications.spec.resources.cpu.request", "1"
        );
        final ApplicationSpecificationConfig specConfig =
            ConfigUtils.createWithProperties(properties, ApplicationSpecificationConfig.class);
        assertThat(specConfig.getImagePullPolicy().getPolicyName()).isEqualTo(ImagePullPolicy.ALWAYS.getPolicyName());

        assertThat(specConfig.getResources().getMemory().getLimit()).isEqualTo("200Mi");
        assertThat(specConfig.getResources().getMemory().getRequest()).isEqualTo("150Mi");
        assertThat(specConfig.getResources().getCpu().getLimit()).isEqualTo("5");
        assertThat(specConfig.getResources().getCpu().getRequest()).isEqualTo("1");
    }
}
