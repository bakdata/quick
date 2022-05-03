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

import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.bakdata.quick.common.exception.NotFoundException;
import com.bakdata.quick.manager.k8s.middleware.Middleware;
import com.bakdata.quick.manager.k8s.resource.QuickResource;
import com.bakdata.quick.manager.k8s.resource.QuickResources;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.micronaut.http.HttpStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom k8s client.
 */
@Singleton
@Slf4j
public class KubernetesManagerClient {
    private final String namespace;
    private final KubernetesClient client;

    /**
     * Injectable constructor.
     *
     * @param client underlying k8s client
     */
    @Inject
    public KubernetesManagerClient(final KubernetesClient client) {
        this.namespace = client.getNamespace();
        this.client = client;
        KubernetesDeserializer.registerCustomKind(Middleware.API_VERSION, Middleware.KIND, Middleware.class);
    }

    /**
     * Deletes all the resources passed in.
     *
     * @param quickResources A quick resources object containing one or many kubernetes resources
     * @return Completable or throws an exception if fail
     */
    public Completable delete(final QuickResources quickResources) {
        return Completable.fromCallable(() -> {
            final KubernetesList resourceList = getKubernetesList(quickResources);
            return this.client.resourceList(resourceList).inNamespace(this.namespace).delete();
        });
    }

    /**
     * Deploys all the resources passed in.
     *
     * @param quickResources A quick resources object containing one or many kubernetes resources
     * @return Returns a Completable or throws an exception if specific resources already exist
     */
    public Completable deploy(final QuickResources quickResources) {
        return Completable.fromCallable(() -> {
            final KubernetesList resourceList = getKubernetesList(quickResources);
            final List<HasMetadata> resourcesMetadata = this.client.resourceList(resourceList)
                    .inNamespace(this.namespace).fromServer().get();
            if (resourcesMetadata.stream().allMatch(Objects::isNull)) {
                return this.client.resourceList(resourceList).inNamespace(this.namespace).createOrReplace();
            } else {
                final String names = resourcesMetadata.stream()
                        .filter(Objects::nonNull)
                        .map(singleResourceMetadata -> singleResourceMetadata.getMetadata().getName())
                        .collect(Collectors.joining(", "));
                throw new BadArgumentException(String.format("Following resources already exist: %s", names));
            }
        });
    }

    /**
     * Deploys a new job to k8s.
     */
    public Completable deploy(final Job appDeletionJob) {
        return Completable.fromAction(() -> this.client.batch().v1().jobs().create(appDeletionJob))
            .onErrorResumeNext(e -> handleDeploymentError(e, ResourceType.JOB, appDeletionJob));
    }

    /**
     * Checks if a quick resource exists in the cluster or not.
     *
     * @param prefix determines the quick resource prefix {@link ResourcePrefix}
     * @param name   the name of the resource on the cluster
     * @return Completable and if the resource does not exist {@link NotFoundException} is thrown
     */
    public Completable checkDeploymentExistence(final ResourcePrefix prefix, final String name) {
        final String resourceName = prefix.getPrefix() + name;
        final Single<Deployment> deployment = this.readDeployment(resourceName);
        return deployment.ignoreElement().onErrorResumeNext(error -> {
            // readDeployment didn't find a resource with that name and threw a null pointer
            if (error instanceof NullPointerException) {
                return Completable.error(new NotFoundException(String.format("Could not find resource %s", name)));
            } else {
                return Completable.error(error);
            }
        });
    }

    /**
     * Updates a config map in k8s.
     */
    public Completable updateConfigMap(final String name, final Map<String, String> data) {
        log.info("updating the config map {} with new schema", name);
        return Completable.fromAction(
                () -> {
                    final ConfigMap configMap = this.client.configMaps().withName(name).get();
                    configMap.setData(data);
                    this.client.configMaps().createOrReplace(configMap);
                    log.info("Successfully updated gateway");
                })
            .onErrorResumeNext(ex -> {
                log.error("Could not update configmap for {}", name, ex);
                return handleDeletionError(ex, name, ResourceType.CONFIG_MAP);
            });
    }

    /**
     * Returns the deployment if it exists in k8s.
     */
    public Single<Deployment> readDeployment(final String name) {
        return Single.fromCallable(() -> this.client.apps().deployments().withName(name).get());
    }

    /**
     * Lists all existing k8s deployments.
     */
    public Single<List<Deployment>> listDeployments(final String startsWith) {
        return Single.fromCallable(() -> {
            final List<Deployment> listDeployment = this.client.apps().deployments().list().getItems().stream()
                .filter(deployment -> deployment.getMetadata().getName().startsWith(startsWith))
                .collect(Collectors.toList());
            if (listDeployment == null) {
                throw new NotFoundException(String.format("Could not find resource %s", startsWith));
            }
            return listDeployment;
        });
    }

    private static KubernetesList getKubernetesList(final QuickResources quickResources) {
        final List<HasMetadata> resources = quickResources.listResources().stream()
            .map(QuickResource::getResource)
            .collect(Collectors.toList());

        final KubernetesList resourceList = new KubernetesList();
        resourceList.setItems(resources);
        return resourceList;
    }

    private static Completable handleDeploymentError(final Throwable ex, final ResourceType type,
                                                     final HasMetadata object) {
        if (ex instanceof KubernetesClientException) {
            final String reason = ((KubernetesClientException) ex).getStatus().getReason();
            return Completable.error(new BadArgumentException(reason));
        }
        final String resourceName = object.getMetadata().getName();
        final String errorMessage = String.format("%s %s could not be deployed.", type.getName(), resourceName);
        return Completable.error(new InternalErrorException(errorMessage));
    }

    /**
     * Handles errors that were thrown during the deletion of a resource.
     *
     * <p>
     * In most cases, an error occurs when a user attempts to delete a non-existing resource (e.g. because of a typo).
     *
     * @param ex   exception thrown during attempted deletion of resource
     * @param name name of the resource to delete
     * @param type type of the resource
     * @return completable with new error
     */
    private static Completable handleDeletionError(final Throwable ex, final String name, final ResourceType type) {
        if (ex instanceof KubernetesClientException) {
            final KubernetesClientException k8sEx = (KubernetesClientException) ex;
            if (k8sEx.getStatus().getCode() == HttpStatus.NOT_FOUND.getCode()) {
                return Completable.error(new NotFoundException("Resource not found"));
            }
            log.warn("Kubernetes error during deletion:", k8sEx);
        }

        // return generic error message; something is seriously wrong
        final String errorMessage = String.format("Could not delete %s %s", type.getName(), name);
        return Completable.error(new InternalErrorException(errorMessage));
    }

    enum ResourceType {
        DEPLOYMENT("Deployment"),
        SERVICE("Service"),
        CONFIG_MAP("Config Map"),
        JOB("Deletion Job");

        private final String name;

        ResourceType(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
