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

package com.bakdata.quick.manager.gateway.resource;

import com.bakdata.quick.manager.k8s.resource.KubernetesErrorHandler;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResourceErrorHandler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Config map holding the gateway's schema.
 */
public class GatewayConfigMap implements QuickResource {
    private final ConfigMap configMap;
    private final QuickResourceErrorHandler errorHandler;

    public GatewayConfigMap(final ConfigMap configMap) {
        this.configMap = configMap;
        this.errorHandler = new KubernetesErrorHandler(configMap.getMetadata().getName(), configMap.getKind());
    }

    @Override
    public HasMetadata getResource() {
        return this.configMap;
    }

    @Override
    public QuickResourceErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
