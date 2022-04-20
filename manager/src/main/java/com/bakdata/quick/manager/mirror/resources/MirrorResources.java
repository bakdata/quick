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

package com.bakdata.quick.manager.mirror.resources;

import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResources;
import java.util.List;

/**
 * This class implements the QuickResources interface and holds the logic for all the quick Mirror resources. The
 * Mirror resources are listed bellow:
 * <ul>
 *   <li>Deployment {@link MirrorDeployment}</li>
 *   <li>Service {@link MirrorService}</li>
 * </ul>
 */
public class MirrorResources implements QuickResources {
    public static final String MIRROR_IMAGE = "quick-mirror";

    private final MirrorDeployment deployment;
    private final MirrorService service;

    public MirrorResources(final MirrorDeployment deployment,
        final MirrorService service) {
        this.deployment = deployment;
        this.service = service;
    }

    @Override
    public List<QuickResource> listResources() {
        return List.of(this.deployment, this.service);
    }
}
