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

package com.bakdata.quick.manager.k8s.resource;

import io.micronaut.core.util.StringUtils;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * This interface describes the quick resources. The following quick resources need to implement this interface:
 * <ul>
 *   <li>Gateway {@link com.bakdata.quick.manager.gateway.resource.GatewayResources}</li>
 *   <li>Mirror {@link com.bakdata.quick.manager.mirror.resources.MirrorResources}</li>
 *   <li>Application {@link com.bakdata.quick.manager.application.resources.ApplicationResources}</li>
 * </ul>
 */
public interface QuickResources {
    default List<QuickResource> listResources() {
        return Collections.emptyList();
    }

    /**
     * Collection for prefixes used for deploying quick resources.
     */
    enum ResourcePrefix {
        GATEWAY("quick-gateway-"),
        APPLICATION("quick-app-"),
        MIRROR("quick-mirror-"),
        INTERNAL(StringUtils.EMPTY_STRING);

        @Getter
        private final String prefix;

        ResourcePrefix(final String deploymentPrefix) {
            this.prefix = deploymentPrefix;
        }
    }

    /**
     * Retrieves the name of resources (without prefix).
     *
     * @return The name that a user has chosen for a specific type of resources.
     */
    String getResourcesName();

}
