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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.bakdata.quick.common.api.model.manager.creation.MirrorArguments;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.manager.TestUtil;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MirrorResourceLoaderTest extends KubernetesTest {
    private static final String EXPECTED_MIRROR_IMAGE = "quick-mirror";
    private static final String DEFAULT_NAME = "test";
    private static final String DEFAULT_DEPLOYMENT_NAME = ResourcePrefix.MIRROR.getPrefix() + DEFAULT_NAME;
    private static final String DEFAULT_TOPIC_NAME = "test_topic";

    private MirrorResourceLoader loader = null;

    @BeforeEach
    void setUp() {
        this.loader = new MirrorResourceLoader(new KubernetesResources(),
            this.getDeploymentConfig(),
            TestUtil.newAppSpec());
    }

    @Test
    void shouldCreateMirrorDeployment() {
        final ImageConfig imageConfig = ImageConfig.of(DOCKER_REGISTRY, EXPECTED_MIRROR_IMAGE, 3, DEFAULT_IMAGE_TAG);

        final MirrorArguments mirrorArguments = new MirrorArguments(Duration.of(1, ChronoUnit.MINUTES), null, null);

        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            3,
            null,
            mirrorArguments);

        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        final Map<String, String> labels = Map.of(
            "app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
            "app.kubernetes.io/managed-by", "quick",
            "app.kubernetes.io/component", "mirror"
        );

        final Map<String, String> selectors = Map.of(
            "app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
            "app.kubernetes.io/component", "mirror"
        );

        final Map<String, String> annotations = Map.of(
            "d9p.io/fixed-tag", "false"
        );

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                assertThat(deployment.getMetadata())
                    .satisfies(metadata -> {
                        assertThat(metadata.getName()).isEqualTo(DEFAULT_DEPLOYMENT_NAME);
                        assertThat(metadata.getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
                        assertThat(metadata.getAnnotations()).containsExactlyInAnyOrderEntriesOf(annotations);
                    });

                assertThat(deployment.getSpec())
                    .satisfies(spec -> {
                        assertThat(spec.getReplicas()).isEqualTo(3);
                        assertThat(spec.getSelector().getMatchLabels()).containsExactlyInAnyOrderEntriesOf(selectors);
                        assertThat(spec.getTemplate().getMetadata().getLabels()).containsExactlyInAnyOrderEntriesOf(
                            labels);
                    });

                final DeploymentSpec deploymentSpec = deployment.getSpec();
                assertThat(deploymentSpec)
                    .hasFieldOrPropertyWithValue("replicas", 3)
                    .extracting(spec -> spec.getTemplate().getMetadata().getLabels(), MAP)
                    .containsExactlyInAnyOrderEntriesOf(labels);

                final PodSpec podSpec = deploymentSpec.getTemplate().getSpec();

                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("image", imageConfig.asImageString())
                    .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
                    .hasFieldOrPropertyWithValue("name", DEFAULT_DEPLOYMENT_NAME)
                    .satisfies(container -> {
                        assertThat(container.getPorts())
                            .isNotNull()
                            .hasSize(1)
                            .first()
                            .hasFieldOrPropertyWithValue("containerPort", KubernetesResources.CONTAINER_PORT);

                        assertThat(container.getArgs())
                            .contains(String.format("--input-topics=%s", DEFAULT_TOPIC_NAME), "--retention-time=PT1M");

                        assertThat(container.getEnv())
                            .isNotNull()
                            .hasSize(2)
                            .first()
                            .hasFieldOrPropertyWithValue("name", "POD_IP")
                            .extracting(EnvVar::getValueFrom)
                            .isNotNull()
                            .extracting(EnvVarSource::getFieldRef)
                            .hasFieldOrPropertyWithValue("fieldPath", "status.podIP");

                        assertThat(container.getEnvFrom())
                            .isNotNull()
                            .hasSize(1)
                            .first()
                            .extracting(EnvFromSource::getConfigMapRef)
                            .isNotNull()
                            .hasFieldOrPropertyWithValue("name", KubernetesResources.QUICK_CONFIG_NAME);
                    });
            });
    }

    @Test
    void shouldCreateDeploymentWithCustomTopicName() {
        final String topicName = "__internal-test-topic";
        final String name = "test-topic";
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            name,
            topicName,
            1,
            null,
            null);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                final String deploymentName = ResourcePrefix.MIRROR.getPrefix() + name;
                assertThat(deployment.getMetadata())
                    .hasFieldOrPropertyWithValue("name", deploymentName);

                final DeploymentSpec deploymentSpec = deployment.getSpec();
                assertThat(deploymentSpec.getTemplate().getMetadata().getLabels())
                    .contains(Map.entry("app.kubernetes.io/name", deploymentName));

                final PodSpec podSpec = deploymentSpec.getTemplate().getSpec();

                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", deploymentName)
                    .extracting(Container::getArgs, LIST)
                    .contains("--input-topics=" + topicName);
            });
    }

    @Test
    void shouldCreateDeploymentWithCustomTag() {
        final String customTag = "my-tag";
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            customTag,
            null);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("image", getImage(EXPECTED_MIRROR_IMAGE, customTag));
            });
    }

    @Test
    void shouldSetReplicasForDeployment() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            5,
            null,
            null);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final DeploymentSpec deploymentSpec = deployment.getSpec();
                assertThat(deploymentSpec)
                    .hasFieldOrPropertyWithValue("replicas", 5);
            });
    }

    @Test
    void shouldSetRetentionTimeForDeployment() {
        final Duration retentionTime = Duration.ofHours(1);
        final MirrorArguments mirrorArguments = new MirrorArguments(retentionTime, null, null);
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            null,
            mirrorArguments);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .extracting(Container::getArgs, LIST)
                    .hasSize(2)
                    .contains("--input-topics=" + DEFAULT_TOPIC_NAME)
                    .contains("--retention-time=" + retentionTime);
            });
    }

    @Test
    void shouldSetRangeFieldForMirrorDeployment() {
        final String rangeField = "timestamp";
        final MirrorArguments mirrorArguments = new MirrorArguments(null, rangeField, null);
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            null,
            mirrorArguments);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .extracting(Container::getArgs, LIST)
                    .hasSize(2)
                    .contains("--input-topics=" + DEFAULT_TOPIC_NAME)
                    .contains("--range-field=" + rangeField);
            });
    }

    @Test
    void shouldSetRangeKeyAndRangeFieldForMirrorDeployment() {
        final String rangeKey = "userId";
        final String rangeField = "timestamp";
        final MirrorArguments mirrorArguments = new MirrorArguments(null, rangeField, rangeKey);
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            null,
            mirrorArguments);
        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .extracting(Container::getArgs, LIST)
                    .hasSize(3)
                    .contains("--input-topics=" + DEFAULT_TOPIC_NAME)
                    .contains("--range-field=" + rangeField)
                    .contains("--range-key=" + rangeKey);
            });
    }

    @Test
    void shouldThrowBadArgumentExceptionWhenBothRetentionTimeAndRangeFieldSet() {
        final Duration retentionTime = Duration.ofHours(1);
        final String rangeField = "timestamp";
        final MirrorArguments mirrorArguments = new MirrorArguments(retentionTime, rangeField, null);

        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            null,
            mirrorArguments);

        assertThatThrownBy(() -> this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR)).isInstanceOf(
                BadArgumentException.class)
            .hasMessageContaining("The --range-field option must not be specified" +
                " when --retention-time is set");
    }

    @Test
    void shouldThrowBadArgumentExceptionWhenRangeKeyIsSetButRangeFieldIsNotSet() {
        final MirrorArguments mirrorArguments = new MirrorArguments(null, null, "test");

        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            1,
            null,
            mirrorArguments);

        assertThatThrownBy(() -> this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR)).isInstanceOf(
                BadArgumentException.class)
            .hasMessageContaining("The --range-key can be set only when a --range-field is set");
    }

    @Test
    void shouldCreateMirrorService() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            DEFAULT_NAME,
            DEFAULT_TOPIC_NAME,
            3,
            "snapshot",
            null);

        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.SERVICE);

        final Map<String, String> labels = Map.of(
            "app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
            "app.kubernetes.io/managed-by", "quick",
            "app.kubernetes.io/component", "mirror"
        );

        final Map<String, String> selectors = Map.of(
            "app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
            "app.kubernetes.io/component", "mirror"
        );

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Service.class))
            .satisfies(service -> {

                assertThat(service.getMetadata())
                    .hasFieldOrPropertyWithValue("name", DEFAULT_DEPLOYMENT_NAME)
                    .extracting(ObjectMeta::getLabels, MAP)
                    .isNotNull()
                    .containsExactlyInAnyOrderEntriesOf(labels);

                final ServiceSpec serviceSpec = service.getSpec();

                assertThat(serviceSpec.getSelector())
                    .containsExactlyInAnyOrderEntriesOf(selectors);

                assertThat(serviceSpec.getPorts())
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("protocol", "TCP")
                    .hasFieldOrPropertyWithValue("port", KubernetesResources.SERVICE_PORT)
                    .hasFieldOrPropertyWithValue("targetPort", new IntOrString(KubernetesResources.CONTAINER_PORT));
            });
    }

    @Test
    void shouldCreateServiceWithCustomTopicName() {
        final String topicName = "__internal-test-topic";
        final String name = "test-topic";
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            name,
            topicName,
            1,
            null,
            null);

        final MirrorResources mirrorResources = this.loader.forCreation(mirrorCreationData, ResourcePrefix.MIRROR);

        final Optional<HasMetadata> hasMetadata = findResource(mirrorResources, ResourceKind.SERVICE);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Service.class))
            .satisfies(service -> {
                final String deploymentName = ResourcePrefix.MIRROR.getPrefix() + name;
                assertThat(service.getMetadata())
                    .hasFieldOrPropertyWithValue("name", deploymentName)
                    .extracting(ObjectMeta::getLabels, MAP)
                    .isNotNull()
                    .contains(Map.entry("app.kubernetes.io/name", deploymentName));

                final ServiceSpec serviceSpec = service.getSpec();

                assertThat(serviceSpec.getSelector())
                    .contains(Map.entry("app.kubernetes.io/name", deploymentName));
            });
    }
}
