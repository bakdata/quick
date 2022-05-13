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

import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.util.CliArgHandler;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.ResourceConfig;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import com.bakdata.quick.manager.k8s.resource.ResourceLoader;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.micronaut.core.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Singleton;
import org.thymeleaf.context.Context;

/**
 * Loader for all kubernetes resources needed for the application.
 *
 * <p>
 * It fills out the Kubernetes template files with the given arguments.
 */
@Singleton
public class ApplicationResourceLoader implements ResourceLoader<ApplicationResources, ApplicationCreationData> {
    private final KubernetesResources kubernetesResources;
    private final KafkaConfig kafkaConfig;
    private final ResourceConfig resourceConfig;
    private final DeploymentConfig deploymentConfig;

    /**
     * Default constructor.
     *
     * @param kubernetesResources underlying engine for loading k8s resources
     * @param resourceConfig config for setting resources for new deployments
     */
    public ApplicationResourceLoader(final KubernetesResources kubernetesResources,
        final KafkaConfig kafkaConfig, final ResourceConfig resourceConfig,
        final DeploymentConfig deploymentConfig) {
        this.kubernetesResources = kubernetesResources;
        this.kafkaConfig = kafkaConfig;
        this.resourceConfig = resourceConfig;
        this.deploymentConfig = deploymentConfig;
    }

    /**
     * Creates resources that can be deployed.
     *
     * <p>
     * This function creates all the Kubernetes resources that an application needs for its deployment. Deployment and
     * Service templates are filled with the data passed.
     */
    @Override
    public ApplicationResources forCreation(final ApplicationCreationData appCreationData,
        final ResourcePrefix resourcePrefix) {
        final int replicas =
            Objects.requireNonNullElse(appCreationData.getReplicas(), this.deploymentConfig.getDefaultReplicas());
        final ImageConfig config = ImageConfig.of(appCreationData.getRegistry(), appCreationData.getImageName(),
            replicas, appCreationData.getTag());
        final String applicationName = appCreationData.getName();
        final String deploymentName = getDeploymentName(applicationName);
        final Map<String, String> arguments = Objects.requireNonNullElse(appCreationData.getArguments(), Map.of());
        final List<String> listArgs = CliArgHandler.convertArgs(arguments, this.kafkaConfig);

        final ApplicationDeployment deployment =
            new ApplicationDeployment(this.createAppDeployment(deploymentName, listArgs, config, this.resourceConfig,
                appCreationData.getPort(),  appCreationData.getImagePullSecret()));

        if (appCreationData.getPort() != null) {
            final ApplicationService service =
                new ApplicationService(this.createApplicationService(deploymentName, appCreationData.getPort()));
            return new ApplicationResources(applicationName, deployment, Optional.of(service));
        }
        return new ApplicationResources(applicationName, deployment, Optional.empty());
    }

    /**
     * Creates resources that can be used to delete them remotely.
     *
     * <p>
     * This function is responsible for creating a dummy {@link ApplicationResources} object with the given name. After
     * creating the resource objects the Kubernetes client is able to delete all the passed resources in this function
     * at once.
     */
    @Override
    public ApplicationResources forDeletion(final String name) {
        final ApplicationDeployment deployment =
            new ApplicationDeployment(KubernetesResources.forDeletion(Deployment.class, name));

        final ApplicationService service =
            new ApplicationService(KubernetesResources.forDeletion(Service.class, name));

        return new ApplicationResources(name, deployment, Optional.of(service));
    }

    /**
     * Creates a k8s application deployment for stream-bootstrap apps.
     *
     * @param name deployment name
     * @param arguments additional arguments passed to the app through the CLI
     * @param imageConfig configuration for the deployed image
     * @param resourceConfig memory + cpu requests and limits to use
     */
    private Deployment createAppDeployment(final String name, final List<String> arguments,
        final ImageConfig imageConfig, final ResourceConfig resourceConfig, @Nullable final Integer port,
                                           @Nullable final String imagePullSecret) {
        final Context root = new Context();
        root.setVariable("name", name);
        root.setVariable("args", arguments);
        root.setVariable("image", imageConfig.asImageString());
        // todo set release name correctly
        root.setVariable("releaseName", "quick-dev");
        root.setVariable("replicas", imageConfig.getReplicas());
        // todo set pull policy through image config
        root.setVariable("pullPolicy", "Always");
        root.setVariable("resourceConfig", resourceConfig);
        root.setVariable("port", port);
        root.setVariable("hasService", !Objects.isNull(port));
        root.setVariable("imagePullSecret", imagePullSecret);
        return this.kubernetesResources.loadResource(root, "streamsApp/deployment", Deployment.class);
    }

    /**
     * Creates a k8s service for an app.
     *
     * @param name the name of the app (and the service)
     * @param port the container port
     */
    private Service createApplicationService(final String name, @Nullable final Integer port) {
        final Context root = new Context();
        root.setVariable("name", name);
        root.setVariable("port", port);
        return this.kubernetesResources.loadResource(root, "streamsApp/service", Service.class);
    }

    public static String getDeploymentName(final String applicationName) {
        return String.format("%s%s", ResourcePrefix.APPLICATION.getPrefix(), applicationName);
    }
}
