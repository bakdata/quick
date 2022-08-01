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

package com.bakdata.quick.manager.mirror;

import static com.bakdata.quick.manager.mirror.resources.MirrorResources.MIRROR_IMAGE;

import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesManagerClient;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import com.bakdata.quick.manager.mirror.resources.MirrorResourceLoader;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Singleton;

/**
 * Service for handling mirror applications on k8s cluster.
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
public class KubernetesMirrorService implements MirrorService {

    private final KubernetesResources resources;
    private final KubernetesManagerClient kubeClient;
    private final DeploymentConfig deploymentConfig;
    private final MirrorResourceLoader loader;

    /**
     * Injectable constructor.
     */
    public KubernetesMirrorService(final KubernetesResources resources,
        final KubernetesManagerClient kubernetesManagerClient,
        final DeploymentConfig deploymentConfig,
        final MirrorResourceLoader loader) {
        this.resources = resources;
        this.kubeClient = kubernetesManagerClient;
        this.deploymentConfig = deploymentConfig;
        this.loader = loader;
    }

    @Override
    public Completable createMirror(final MirrorCreationData mirrorCreationData) {
        return this.create(mirrorCreationData, ResourcePrefix.MIRROR);
    }

    @Override
    public Completable createInternalMirror(final MirrorCreationData mirrorCreationData) {
        return this.create(mirrorCreationData, ResourcePrefix.INTERNAL);
    }

    @Override
    public Completable deleteMirror(final String name) {
        final String deploymentName = MirrorResourceLoader.getDeploymentName(name);
        final ImageConfig imageConfig = ImageConfig
            .of(this.deploymentConfig.getDockerRegistry(), MIRROR_IMAGE, 1, this.deploymentConfig.getDefaultImageTag());

        // first extract info about mirror deployment
        // we need this to properly delete all kafka related resources like the internal state store topic
        final Single<Deployment> deployment = this.kubeClient.readDeployment(deploymentName);

        // as well as all kafka related resources
        final Completable kafkaCleanUp = deployment
            .map(d -> d.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs())
            .map(list -> this.resources.createDeletionJob(deploymentName, imageConfig.asImageString(), list))
            .flatMapCompletable(this.kubeClient::deploy);

        final Completable resourceDeletion = Single.fromCallable(() -> this.loader.forDeletion(deploymentName))
            .flatMapCompletable(this.kubeClient::delete);

        return kafkaCleanUp.andThen(resourceDeletion);
    }

    private Completable create(final MirrorCreationData mirrorCreationData, final ResourcePrefix prefix) {
        return Single.fromCallable(() -> this.loader.forCreation(mirrorCreationData, prefix))
            .flatMapCompletable(this.kubeClient::deploy);
    }
}
