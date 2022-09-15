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

import static com.bakdata.quick.manager.gateway.resource.GatewayResources.GATEWAY_IMAGE;
import static com.bakdata.quick.manager.gateway.resource.GatewayResources.getConfigMapName;
import static com.bakdata.quick.manager.gateway.resource.GatewayResources.getResourceName;

import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.manager.config.ApplicationSpecificationConfig;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.middleware.Middleware;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import com.bakdata.quick.manager.k8s.resource.ResourceLoader;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.Context;

/**
 * Loader for all kubernetes resources needed for the gateway.
 *
 * <p>
 * It fills out the Kubernetes template files with the given arguments.
 */
@Singleton
@Slf4j
public class GatewayResourceLoader implements ResourceLoader<GatewayResources, GatewayCreationData> {

    private static final String DEFAULT_INDENTATION = "  ";
    private final KubernetesResources kubernetesResources;
    private final DeploymentConfig deploymentConfig;
    private final ApplicationSpecificationConfig appSpecConfig;
    private final String namespace;

    /**
     * Default constructor.
     *
     * @param kubernetesResources underlying engine for loading k8s resources
     * @param deploymentConfig config for deploying new resources
     * @param appSpecConfig config for setting resources for new deployments
     * @param client k8s manager client
     */
    public GatewayResourceLoader(final KubernetesResources kubernetesResources, final DeploymentConfig deploymentConfig,
        final ApplicationSpecificationConfig appSpecConfig, final KubernetesClient client) {
        this.kubernetesResources = kubernetesResources;
        this.deploymentConfig = deploymentConfig;
        this.appSpecConfig = appSpecConfig;
        this.namespace = client.getNamespace();
    }

    /**
     * Constructor for testing.
     *
     * @param kubernetesResources underlying engine for loading k8s resources
     * @param deploymentConfig config for deploying new resources
     * @param appSpecConfig config for setting resources for new deployments
     * @param namespace namespace the new resources should be deployed in
     */
    @VisibleForTesting
    public GatewayResourceLoader(final KubernetesResources kubernetesResources, final DeploymentConfig deploymentConfig,
        final ApplicationSpecificationConfig appSpecConfig, final String namespace) {
        this.kubernetesResources = kubernetesResources;
        this.deploymentConfig = deploymentConfig;
        this.appSpecConfig = appSpecConfig;
        this.namespace = namespace;
    }

    /**
     * Creates resources that can be deployed.
     *
     * <p>
     * This function creates all the Kubernetes resources that a gateway needs for its deployment. Deployment, Service,
     * Ingress, Middleware, and ConfigMap templates are filled with the data passed through the
     * {@link GatewayCreationData}.
     */
    @Override
    public GatewayResources forCreation(final GatewayCreationData gatewayCreationData,
        final ResourcePrefix resourcePrefix) {
        final String imageTag =
            Objects.requireNonNullElse(gatewayCreationData.getTag(), this.deploymentConfig.getDefaultImageTag());
        final int imageReplicas =
            Objects.requireNonNullElse(gatewayCreationData.getReplicas(),
                this.deploymentConfig.getDefaultReplicas());

        final String dockerRegistry = this.deploymentConfig.getDockerRegistry();
        final ImageConfig imageConfig = ImageConfig.of(dockerRegistry, GATEWAY_IMAGE, imageReplicas, imageTag);
        final String gatewayName = gatewayCreationData.getName();
        final String resourceName = getResourceName(gatewayName);
        final boolean hasFixedTag = gatewayCreationData.getTag() != null;

        final GatewayDeployment deployment = new GatewayDeployment(
            this.createGatewayDeployment(resourceName, imageConfig, this.appSpecConfig, hasFixedTag));

        final GatewayService service = new GatewayService(this.createGatewayService(resourceName));

        final GatewayIngress ingress = new GatewayIngress(
            this.createGatewayIngress(resourceName, gatewayCreationData.getName(), this.namespace,
                this.deploymentConfig.getIngressHost(), this.deploymentConfig.isIngressSsl(),
                this.deploymentConfig.getIngressEntrypoint()));

        final GatewayMiddleware middleware =
            new GatewayMiddleware(this.createGatewayMiddleware(resourceName, gatewayCreationData.getName()));

        final GatewayConfigMap configMap =
            new GatewayConfigMap(this.createGatewayConfigMap(resourceName, gatewayCreationData.getSchema()));

        return new GatewayResources(gatewayName, deployment, service, ingress, middleware, configMap);
    }

    /**
     * Creates resources that can be used to delete them remotely.
     *
     * <p>
     * This function is responsible for creating a dummy {@link GatewayResources} object with the given name. After
     * creating the resource objects the Kubernetes client is able to delete all the passed resources in this function
     * at once.
     */
    @Override
    public GatewayResources forDeletion(final String name) {
        final String resourceName = getResourceName(name);

        final GatewayDeployment deployment =
            new GatewayDeployment(KubernetesResources.forDeletion(Deployment.class, resourceName));
        final GatewayService service =
            new GatewayService(KubernetesResources.forDeletion(Service.class, resourceName));
        final GatewayIngress ingress =
            new GatewayIngress(KubernetesResources.forDeletion(Ingress.class, resourceName));
        final GatewayMiddleware middleware =
            new GatewayMiddleware(KubernetesResources.forDeletion(Middleware.class, resourceName));
        final GatewayConfigMap configMap =
            new GatewayConfigMap(KubernetesResources.forDeletion(ConfigMap.class, getConfigMapName(name)));

        return new GatewayResources(name, deployment, service, ingress, middleware, configMap);
    }

    /**
     * Creates a gateway k8s deployment.
     *
     * @param name deployment name
     * @param imageConfig configuration of the gateway image that should be used
     * @param appSpecConfig memory + cpu requests and limits to use
     * @param hasFixedTag true if tag is manually set by user
     */
    private Deployment createGatewayDeployment(final String name, final ImageConfig imageConfig,
        final ApplicationSpecificationConfig appSpecConfig, final boolean hasFixedTag) {
        final Context root = new Context();
        root.setVariable("name", name);
        root.setVariable("image", imageConfig.asImageString());
        root.setVariable("hasFixedTag", hasFixedTag);
        root.setVariable("replicas", imageConfig.getReplicas());
        root.setVariable("pullPolicy", appSpecConfig.getImagePullPolicy().getPolicyName());
        root.setVariable("resourceConfig", appSpecConfig.getResources());
        return this.kubernetesResources.loadResource(root, "gateway/deployment", Deployment.class);
    }

    /**
     * Creates a k8s service for a gateway.
     */
    private Service createGatewayService(final String name) {
        final Context root = new Context();
        root.setVariable("name", name);
        return this.kubernetesResources.loadResource(root, "gateway/service", Service.class);
    }

    /**
     * Creates a k8s ingress for a gateway.
     *
     * @param deploymentName name of the gateway deployment the ingress should route to
     * @param pathName path fragment to use, e.g. {@code d9p.io/gateway/<pathName>/}
     * @param ingressHost host of the deployment
     * @param ingressSsl whether the ingress should use ssl
     * @param ingressEntrypoint entrypoint for Traefik
     */
    private Ingress createGatewayIngress(final String deploymentName, final String pathName,
        final String namespace, final Optional<String> ingressHost,
        final boolean ingressSsl, final String ingressEntrypoint) {
        final Context root = new Context();
        root.setVariable("deploymentName", deploymentName);
        root.setVariable("pathName", pathName);
        root.setVariable("namespace", namespace);
        ingressHost.ifPresent(host -> root.setVariable("host", host));
        root.setVariable("ingressSsl", ingressSsl);
        root.setVariable("ingressEntrypoint", ingressEntrypoint);
        return this.kubernetesResources.loadResource(root, "gateway/ingress", Ingress.class);
    }

    /**
     * Creates a k8s middleware (traefik CRD) for the gateway.
     *
     * <p>
     * A middleware is CRD provided by traefik, see <a href="https://doc.traefik.io/traefik/middlewares/overview/">here
     * for more information.</a> We use it to strip the prefix "gateway/gatewayName": When a client sends a request to
     * http://d9p.io/gateway/my-gateway/graphql, the gateway sees http://host/graphql.
     *
     * @param deploymentName gateway deployment name
     * @param pathName gateway path name
     */
    private Middleware createGatewayMiddleware(final String deploymentName, final String pathName) {
        final Context root = new Context();
        root.setVariable("deploymentName", deploymentName);
        root.setVariable("pathName", pathName);
        return this.kubernetesResources.loadResource(root, "gateway/middleware", Middleware.class);
    }

    /**
     * Creates a configmap where the gateway's GraphQL can be stored in.
     *
     * <p>
     * The config map is created with a schema.graphql data as a key and an optional value. The value is only set if the
     * schema is given during creation.
     */
    private ConfigMap createGatewayConfigMap(final String name, @Nullable final String schema) {
        final Context root = new Context();
        root.setVariable("name", name);
        if (schema != null) {
            final String reformattedSchema = this.formatSchemaForYaml(schema);
            log.debug("Creating gateway config map with the following schema: {}", reformattedSchema);
            root.setVariable("schema", reformattedSchema);
        }
        return this.kubernetesResources.loadResource(root, "gateway/config-map", ConfigMap.class);
    }

    private String formatSchemaForYaml(final String schema) {
        return schema.lines().map(line -> DEFAULT_INDENTATION + line).collect(Collectors.joining());
    }
}
