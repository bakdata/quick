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

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;

/**
 * Configuration setting resources for k8s deployments.
 */
@ConfigurationProperties(ResourceConfig.QUICK_APPLICATIONS_RESOURCES)
@Getter
public class ResourceConfig {
    public static final String QUICK_APPLICATIONS_RESOURCES = "quick.applications.resources";
    private final Memory memory;
    private final Cpu cpu;

    @ConfigurationInject
    public ResourceConfig(final Memory memory, final Cpu cpu) {
        this.memory = memory;
        this.cpu = cpu;
    }

    /**
     * Memory request and limit.
     */
    @ConfigurationProperties("memory")
    public interface Memory extends Resource {}

    /**
     * CPU request and limit.
     */
    @ConfigurationProperties("cpu")
    public interface Cpu extends Resource {}

    /**
     * K8s request and limit holder.
     */
    public interface Resource {
        String getLimit();

        String getRequest();
    }

}
