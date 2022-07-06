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

import com.bakdata.quick.manager.TestUtil;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.middleware.Middleware;
import com.bakdata.quick.manager.k8s.middleware.MiddlewareList;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResources;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Provides k8s integration for tests.
 */
@Getter
public abstract class KubernetesTest {
    public static final String NAMESPACE = "test";
    public static final String DOCKER_REGISTRY = "registry.hub.docker.com";
    public static final String DEFAULT_IMAGE_TAG = "latest";

    protected final KubernetesServer kubernetesServer = new KubernetesServer(false, true);
    protected KubernetesClient client = null;

    private DeploymentConfig deploymentConfig = null;
    private KubernetesManagerClient managerClient = null;
    private ResourceConfig resourceConfig = null;

    /**
     * Set up new k8s server and config.
     */
    @BeforeEach
    public void setUpTests() {
        this.kubernetesServer.before();
        this.client = this.kubernetesServer.getClient();
        this.deploymentConfig =
            new DeploymentConfig(DOCKER_REGISTRY, DEFAULT_IMAGE_TAG, 1, Optional.of("quick.host.io"), true,
                "websecure");
        this.managerClient = new KubernetesManagerClient(this.client);
        this.resourceConfig = TestUtil.newResourceConfig();
    }

    @AfterEach
    public void tearDownTests() {
        this.kubernetesServer.after();
    }

    /**
     * Configuration classes for k8s.
     */
    @Data
    public static class ServiceConfig {
        final DeploymentConfig config;
        final KubernetesManagerClient client;
        final ResourceConfig resourceConfig;
    }

    /**
     * @return all k8s services in test namespace.
     */
    protected List<Service> getServices() {
        return this.client.services().inNamespace(NAMESPACE).list().getItems();
    }

    /**
     * @return all k8s deployments in test namespace.
     */
    protected List<Deployment> getDeployments() {
        return this.client.apps()
            .deployments()
            .inNamespace(NAMESPACE)
            .list()
            .getItems();
    }

    /**
     * @return all k8s Ingress in test namespace.
     */
    protected List<Ingress> getIngressItems() {
        return this.client.network().v1().ingresses().inNamespace(NAMESPACE).list().getItems();
    }

    /**
     * @return all k8s ConfigMaps in test namespace.
     */
    protected List<ConfigMap> getConfigMaps() {
        return this.client.configMaps().inNamespace(NAMESPACE).list().getItems();
    }

    /**
     * @return all k8s Middlewares in test namespace.
     */
    protected MiddlewareList getMiddlewares() {
        return this.client.resources(
                Middleware.class,
                MiddlewareList.class
            )
            .list();
    }

    protected static Optional<HasMetadata> findResource(final QuickResources quickResources,
                                                        final ResourceKind resourceKind) {
        return quickResources.listResources().stream().map(QuickResource::getResource)
            .filter(metadata -> resourceKind.getResourceKind().equals(metadata.getKind())).findFirst();
    }

    /**
     * @return A generated string for the deployment image spec. The format is [DOCKER_REGISTRY]/[IMAGE_NAME]:[TAG]
     */
    protected static String getImage(final String dockerRegistry, final String imageName, final String imageTag) {
        return String.format("%s/%s:%s", dockerRegistry, imageName, imageTag);
    }

    protected enum ResourceKind {
        DEPLOYMENT("Deployment"),
        SERVICE("Service"),
        INGRESS("Ingress"),
        CONFIGMAP("ConfigMap"),
        MIDDLEWARE("Middleware");

        @Getter
        private final String resourceKind;

        ResourceKind(final String resourceKind) {
            this.resourceKind = resourceKind;
        }
    }
}
