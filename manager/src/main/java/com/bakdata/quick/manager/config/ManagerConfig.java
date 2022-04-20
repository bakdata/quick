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
import lombok.Getter;

/**
 * Configuration for manager start up behavior.
 */
@ConfigurationProperties("quick.manager")
@Getter
public class ManagerConfig {
    private final boolean updateManagedImages;
    private final boolean createTopicRegistry;

    /**
     * Default constructor.
     *
     * @param updateManagedImages true if the manager should update the images of deployed resources to match its own
     *                            tag
     * @param createTopicRegistry true if manager should deploy topic registry
     */
    @ConfigurationInject
    public ManagerConfig(final boolean updateManagedImages, final boolean createTopicRegistry) {
        this.updateManagedImages = updateManagedImages;
        this.createTopicRegistry = createTopicRegistry;
    }
}
