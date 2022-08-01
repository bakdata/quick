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

package com.bakdata.quick.manager.k8s.cluster;

import com.bakdata.quick.manager.config.DeploymentConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Class ensures that images of resources deployed by the manager match the manager's version.
 */
@Requires(property = "quick.manager.update-managed-images", value = StringUtils.TRUE)
@Singleton
@Slf4j
public class ImageUpdater {
    private static final Map<String, String> selectors = Map.of(
        "app.kubernetes.io/managed-by", "quick"
    );
    private static final String TAG_ANNOTATION_KEY = "d9p.io/fixed-tag";
    private final KubernetesClient client;
    private final String defaultImageTag;

    public ImageUpdater(final KubernetesClient client, final DeploymentConfig deploymentConfig) {
        this.client = client;
        this.defaultImageTag = deploymentConfig.getDefaultImageTag();
    }

    @EventListener
    @Async
    public void onStartUp(final StartupEvent event) {
        this.updateManagedDeployments();
    }

    @VisibleForTesting
    void updateManagedDeployments() {
        final List<Deployment> managedDeployments = this.client.apps().deployments()
            .withLabels(selectors)
            .list()
            .getItems();

        final Stream<Deployment> deploymentsToUpdate = managedDeployments.stream()
            .filter(deployment -> {
                final String tagAnnotationValue = deployment.getMetadata().getAnnotations().get(TAG_ANNOTATION_KEY);
                return Boolean.FALSE.toString().equals(tagAnnotationValue);
            });

        deploymentsToUpdate.forEach(this::updateImage);
    }

    private void updateImage(final Deployment deployment) {
        final String name = deployment.getMetadata().getName();
        final List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        final String image = Iterables.getOnlyElement(containers).getImage();
        // Split image tag assuming a string formatted 'registry/imageName:tag'
        // include the ':' because we need it anyways
        final String baseImage = image.substring(0, image.lastIndexOf(':') + 1);
        final String updatedImage = baseImage + this.defaultImageTag;
        log.info("Update image version for deployment {} to {}", name, this.defaultImageTag);
        this.client.apps().deployments()
            .withName(name)
            .rolling()
            .updateImage(updatedImage);
    }
}
