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

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Optional;
import lombok.Getter;

/**
 * Configuration setting specification for k8s deployments.
 */
@ConfigurationProperties(ApplicationSpecificationConfig.QUICK_APPLICATIONS_SPEC)
@Getter
public class ApplicationSpecificationConfig {
    public static final String QUICK_APPLICATIONS_SPEC = "quick.applications.spec";
    private final ImagePullPolicy imagePullPolicy;
    private final HardwareResource resources;

    /**
     * Default constructor.
     *
     * @param imagePullPolicy sets the image pull policy for the manager application deployment
     * @param resources sets the hardware resources (i.e., CPU and memory) for an application
     */
    @ConfigurationInject
    public ApplicationSpecificationConfig(final Optional<ImagePullPolicy> imagePullPolicy,
        final HardwareResource resources) {
        this.imagePullPolicy = imagePullPolicy.orElse(ImagePullPolicy.ALWAYS);
        this.resources = resources;
    }
}
