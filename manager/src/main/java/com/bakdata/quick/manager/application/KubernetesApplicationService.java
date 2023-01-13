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

package com.bakdata.quick.manager.application;

import com.bakdata.quick.common.api.model.manager.ApplicationDescription;
import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import com.bakdata.quick.common.exception.BadServiceAnnotationException;
import com.bakdata.quick.manager.application.resources.ApplicationResourceLoader;
import com.bakdata.quick.manager.application.resources.ApplicationResources;
import com.bakdata.quick.manager.k8s.KubernetesManagerClient;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Singleton;
import java.util.Optional;

/**
 * Service for handling application deployments on k8s cluster.
 */
@Requires(env = Environment.KUBERNETES)
@Singleton
public class KubernetesApplicationService implements ApplicationService {

    private final KubernetesResources resources;
    private final ApplicationResourceLoader loader;
    private final KubernetesManagerClient kubeClient;

    /**
     * Default constructor.
     */
    public KubernetesApplicationService(final KubernetesResources resources,
        final KubernetesManagerClient kubeClient, final ApplicationResourceLoader loader) {
        this.resources = resources;
        this.kubeClient = kubeClient;
        this.loader = loader;
    }

    @Override
    public Single<ApplicationDescription> getApplicationInformation(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Completable deployApplication(final ApplicationCreationData applicationCreationData) {
        return Single.fromCallable(
                () -> this.loader.forCreation(applicationCreationData, ResourcePrefix.APPLICATION))
            .flatMapCompletable(this.kubeClient::deploy);
    }

    @Override
    public Completable deleteApplication(final String name) {
        final String deploymentName = ApplicationResourceLoader.getDeploymentName(name);
        // first extract info about mirror deployment
        // we need this to properly delete all kafka related resources like the internal state store topic
        final Single<Deployment> deployment = this.kubeClient.readDeployment(deploymentName).cache();

        final Completable resourceDeletion = deployment.map(KubernetesApplicationService::checkServiceAnnotation)
            .map(serviceExists -> {
                final ApplicationResources applicationResources = this.loader.forDeletion(deploymentName);
                if (!serviceExists) {
                    return new ApplicationResources(name, applicationResources.getDeployment(), Optional.empty());
                }
                return applicationResources;
            })
            .flatMapCompletable(this.kubeClient::delete);

        // as well as all kafka related resources
        final Completable kafkaCleanUp = deployment
            .map(k8sDeployment -> {
                final Container container = k8sDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
                return this.resources.createDeletionJob(deploymentName, container.getImage(), container.getArgs(),
                    null);
            })
            .flatMapCompletable(this.kubeClient::deploy);

        return kafkaCleanUp.andThen(resourceDeletion);
    }

    /**
     * Checks if the deployed application also has a service. True if it exists and false if it's not.
     */
    private static boolean checkServiceAnnotation(final HasMetadata app) {
        final String annotation = app.getMetadata().getAnnotations().get("d9p.io/has-service");
        if (annotation == null) {
            // do not throw at null for backwards compatibility
            return false;
        } else if (annotation.equalsIgnoreCase("true")) {
            return true;
        } else if (annotation.equalsIgnoreCase("false")) {
            return false;
        }
        throw new BadServiceAnnotationException(
            String.format("Cannot interpret value for deployment annotation d9p.io/has-service: %s", annotation),
            HttpStatus.BAD_REQUEST);
    }
}
