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

package com.bakdata.quick.manager.mirror.resources;

import static com.bakdata.quick.manager.mirror.resources.MirrorResources.MIRROR_IMAGE;

import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.util.CliArgHandler;
import com.bakdata.quick.manager.config.ApplicationSpecificationConfig;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import com.bakdata.quick.manager.k8s.resource.ResourceLoader;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.Context;

/**
 * Loader for all kubernetes resources needed for the mirror.
 *
 * <p>
 * It fills out the Kubernetes template files with the given arguments.
 */
@Singleton
@Slf4j
public class MirrorResourceLoader implements ResourceLoader<MirrorResources, MirrorCreationData> {

    private final KubernetesResources kubernetesResources;
    private final DeploymentConfig deploymentConfig;
    private final ApplicationSpecificationConfig appSpecConfig;

    /**
     * Default constructor.
     *
     * @param kubernetesResources underlying engine for loading k8s resources
     * @param deploymentConfig config for deploying new resources
     * @param appSpecConfig config for setting resources for new deployments
     */
    public MirrorResourceLoader(final KubernetesResources kubernetesResources,
        final DeploymentConfig deploymentConfig, final ApplicationSpecificationConfig appSpecConfig) {
        this.kubernetesResources = kubernetesResources;
        this.deploymentConfig = deploymentConfig;
        this.appSpecConfig = appSpecConfig;
    }

    /**
     * Creates Mirror resources that can be deployed.
     *
     * <p>
     * This function creates all the Kubernetes resources that a mirror needs for its deployment. Deployment and Service
     * templates are filled with the data passed.
     */
    @Override
    public MirrorResources forCreation(final MirrorCreationData mirrorCreationData, final ResourcePrefix prefix) {
        final String mirrorName = mirrorCreationData.getName();
        final String deploymentName = prefix.getPrefix() + mirrorName;
        final String imageTag =
            Objects.requireNonNullElse(mirrorCreationData.getTag(), this.deploymentConfig.getDefaultImageTag());
        final int imageReplicas =
            Objects.requireNonNullElse(mirrorCreationData.getReplicas(), this.deploymentConfig.getDefaultReplicas());

        final ImageConfig config =
            ImageConfig.of(this.deploymentConfig.getDockerRegistry(), MIRROR_IMAGE, imageReplicas, imageTag);
        final boolean hasFixedTag = mirrorCreationData.getTag() != null;

        final List<String> arguments = CliArgHandler.convertArgs(
            createCliArguments(mirrorCreationData.getTopicName(),
                mirrorCreationData.getRetentionTime(),
                mirrorCreationData.isPoint(),
                mirrorCreationData.getRangeField()));

        final MirrorDeployment deployment = new MirrorDeployment(
            this.createMirrorDeployment(deploymentName, arguments, config, this.appSpecConfig, hasFixedTag));

        final MirrorService service = new MirrorService(this.createMirrorService(deploymentName));

        return new MirrorResources(mirrorName, deployment, service);
    }

    /**
     * Creates Mirror resources that can be used to delete them remotely.
     *
     * <p>
     * This function is responsible for creating a dummy {@link MirrorResources} object with the given name. After
     * creating the resource objects the Kubernetes client is able to delete all the passed resources in this function
     * at once.
     */
    @Override
    public MirrorResources forDeletion(final String name) {
        final MirrorDeployment deployment =
            new MirrorDeployment(KubernetesResources.forDeletion(Deployment.class, name));
        final MirrorService service = new MirrorService(KubernetesResources.forDeletion(Service.class, name));
        return new MirrorResources(name, deployment, service);
    }

    public static String getDeploymentName(final String name) {
        return ResourcePrefix.MIRROR.getPrefix() + name;
    }

    /**
     * Creates a k8s Mirror deployment.
     *
     * @param deploymentName deployment name
     * @param arguments additional arguments passed to the app through the CLI
     * @param imageConfig configuration for the deployed image
     * @param appSpecConfig memory + cpu requests and limits to use
     * @param hasFixedTag true if tag is manually set by user
     */
    private Deployment createMirrorDeployment(final String deploymentName, final List<String> arguments,
        final ImageConfig imageConfig, final ApplicationSpecificationConfig appSpecConfig, final boolean hasFixedTag) {
        final Context root = new Context();
        root.setVariable("name", deploymentName);
        root.setVariable("args", arguments);
        root.setVariable("image", imageConfig.asImageString());
        root.setVariable("replicas", imageConfig.getReplicas());
        root.setVariable("pullPolicy", appSpecConfig.getImagePullPolicy().getPolicyName());
        root.setVariable("resourceConfig", appSpecConfig.getResources());
        root.setVariable("hasFixedTag", hasFixedTag);
        return this.kubernetesResources.loadResource(root, "mirror/deployment", Deployment.class);
    }

    /**
     * Creates k8s service routing to the mirror container.
     */
    private Service createMirrorService(final String name) {
        final Context root = new Context();
        root.setVariable("name", name);
        return this.kubernetesResources.loadResource(root, "mirror/service", Service.class);
    }

    /**
     * Sets the args for the mirror deployment. Throws {@link BadArgumentException} if no query type (range or point) is
     * defined.
     *
     * @param topic the input topic name
     * @param retentionTime retention time
     * @param rangeField the field where the range index should be build on
     * @return an immutable map of command option and the value
     */
    private static Map<String, String> createCliArguments(final String topic,
        @Nullable final Duration retentionTime,
        final boolean point,
        @Nullable final String rangeField) {
        log.debug("Setting the --input-topics option with topic: {}", topic);
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
            .put("--input-topics", topic);

        log.debug("Setting the --point option to: {}", point);
        builder.put("--point", Boolean.toString(point));

        if (Objects.nonNull(retentionTime)) {
            log.debug("Setting the --retention-time option with topic: {}", retentionTime);
            builder.put("--retention-time", retentionTime.toString());
        }
        if (Objects.nonNull(rangeField)) {
            log.debug("Setting the --range option with field: {}", rangeField);
            builder.put("--range", rangeField);
        }
        if (Objects.isNull(rangeField) && !point) {
            throw new BadArgumentException("At least one query type (--range <Field> or --point) should be defined");
        }

        return builder.build();
    }
}
