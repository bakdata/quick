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

package com.bakdata.quick.manager.application.resources;

import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResources;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * This class implements the QuickResources interface and holds the logic for all the quick Application resources. The
 * Application resources are listed bellow:
 * <ul>
 *   <li>Deployment {@link ApplicationDeployment}</li>
 *   <li>Service {@link ApplicationService}</li>
 * </ul>
 */
public class ApplicationResources implements QuickResources {

    private final String name;
    @Getter
    private final ApplicationDeployment deployment;
    private final Optional<ApplicationService> service;

    public ApplicationResources(final String name,
                                final ApplicationDeployment deployment,
                                final Optional<ApplicationService> service) {
        this.name = name;
        this.deployment = deployment;
        this.service = service;
    }

    @Override
    public List<QuickResource> listResources() {
        return this.service.map(applicationService -> List.of(this.deployment, applicationService))
            .orElseGet(() -> List.of(this.deployment));
    }

    @Override
    public String getResourcesName() {
        return this.name;
    }
}
