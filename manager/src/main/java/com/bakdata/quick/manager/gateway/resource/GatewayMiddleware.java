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

import com.bakdata.quick.manager.k8s.middleware.Middleware;
import com.bakdata.quick.manager.k8s.resource.KubernetesErrorHandler;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResourceErrorHandler;
import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * Middleware object that handles traefik's routing to this gateway.
 */
public class GatewayMiddleware implements QuickResource {
    private final Middleware middleware;
    private final QuickResourceErrorHandler errorHandler;

    public GatewayMiddleware(final Middleware middleware) {
        this.middleware = middleware;
        this.errorHandler = new KubernetesErrorHandler(middleware.getMetadata().getName(), middleware.getKind());
    }

    @Override
    public HasMetadata getResource() {
        return this.middleware;
    }

    @Override
    public QuickResourceErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
