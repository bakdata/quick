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

import com.bakdata.quick.manager.k8s.resource.KubernetesErrorHandler;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResourceErrorHandler;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;

/**
 * Deployment for mirror.
 */
public class MirrorDeployment implements QuickResource {

    private final QuickResourceErrorHandler errorHandler;
    private final Deployment deployment;

    public MirrorDeployment(final Deployment deployment) {
        this.deployment = deployment;
        this.errorHandler = new KubernetesErrorHandler(deployment.getMetadata().getName(), deployment.getKind());
    }

    @Override
    public HasMetadata getResource() {
        return this.deployment;
    }

    @Override
    public QuickResourceErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
