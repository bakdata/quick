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


import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.exception.ServiceUnavailableException;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResources;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Completable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * This class implements the QuickResources interface and holds the logic for all the quick Gateway resources. The
 * Gateway resources are listed bellow:
 * <ul>
 *   <li>Deployment {@link GatewayDeployment}</li>
 *   <li>Service {@link GatewayService}</li>
 *   <li>Ingress {@link GatewayIngress}</li>
 *   <li>Middleware {@link GatewayMiddleware}</li>
 *   <li>ConfigMap {@link GatewayConfigMap}</li>
 * </ul>
 */
@Slf4j
public class GatewayResources implements QuickResources {

    public static final String GATEWAY_IMAGE = "quick-gateway";

    private final String name;
    private final GatewayDeployment deployment;
    private final GatewayService service;
    private final GatewayIngress ingress;
    private final GatewayMiddleware middleware;
    private final GatewayConfigMap configMap;

    /**
     * Injecting all the resources a gateway needs.
     *
     * @param gatewayDeployment For gateway Deployment resource
     * @param gatewayService    For gateway Service resource
     * @param gatewayIngress    For gateway Ingress resource
     * @param middleware        For gateway Middleware resource
     * @param configMap         For gateway ConfigMap resource
     */
    public GatewayResources(final String name, final GatewayDeployment gatewayDeployment,
                            final GatewayService gatewayService, final GatewayIngress gatewayIngress,
                            final GatewayMiddleware middleware, final GatewayConfigMap configMap) {
        this.name = name;
        this.deployment = gatewayDeployment;
        this.service = gatewayService;
        this.ingress = gatewayIngress;
        this.middleware = middleware;
        this.configMap = configMap;
    }

    public static String getResourceName(final String name) {
        return String.format("%s%s", ResourcePrefix.GATEWAY.getPrefix(), name);
    }

    public static String getConfigMapName(final String name) {
        return String.format("%s-%s", getResourceName(name), "config");
    }

    /**
     * Retrieves gateway information from the deployment file.
     *
     * @param deployment of the gateway to read the gateway name, number of replicas, and image tag
     * @return {@link GatewayDescription} an object holding the gateway name, number of replicas, and image tag
     */
    public static GatewayDescription getGatewayDescription(final Deployment deployment) {
        final String gatewayDeploymentName = deployment.getMetadata().getName();
        final String gatewayName = gatewayDeploymentName.replaceFirst(ResourcePrefix.GATEWAY.getPrefix(), "");
        final int gatewayReplicas = deployment.getSpec().getReplicas();
        final String image = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
        final String tag = ImageConfig.getTagFromString(image);
        return new GatewayDescription(gatewayName, gatewayReplicas, tag);
    }

    /**
     * Handles the definition creation error and crates an error message.
     *
     * @param ex          the exception thrown
     * @param gatewayName The name of the gateway
     * @return {@link ServiceUnavailableException} with the desired error message
     */
    public static Completable handleDefinitionCreationError(final Throwable ex, final String gatewayName) {
        if (ex instanceof HttpClientResponseException) {
            final HttpClientResponseException clientResponseException = (HttpClientResponseException) ex;
            if (clientResponseException.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
                // This handles the error when the gateway is in running phase on the cluster but not started
                // internally yet
                return Completable.error(() -> {
                    log.info("Gateway pod is starting internally. Throwing NotFoundException.");
                    final String errorMessage =
                        String.format("The gateway %s is unavailable. Try again later.", gatewayName);
                    return new ServiceUnavailableException(errorMessage);
                });
            }
        }
        return Completable.error(ex);
    }

    @Override
    public List<QuickResource> listResources() {
        return List.of(this.deployment, this.service, this.ingress, this.middleware, this.configMap);
    }

    @Override
    public String getResourcesName() {
        return this.name;
    }


}
