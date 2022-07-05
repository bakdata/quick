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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.manager.TestUtil;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationResourceLoaderTest extends KubernetesTest {
    private static final int DEFAULT_PORT = 8080;
    private static final String NAME = "application-name";
    private static final String DEFAULT_DEPLOYMENT_NAME = ResourcePrefix.APPLICATION.getPrefix() + NAME;
    private static final String DEFAULT_SECRET = "secret";

    private ApplicationResourceLoader loader = null;
    private final KafkaConfig kafkaConfig = new KafkaConfig("bootstrap", "http://dummy:123");

    @BeforeEach
    void setUp() {
        this.loader =
            new ApplicationResourceLoader(new KubernetesResources(),
                this.kafkaConfig,
                TestUtil.newResourceConfig(), this.getDeploymentConfig());
    }

    @Test
    void shouldCreateDeploymentForAppDefaults() {
        final ImageConfig imageConfig = ImageConfig.of(DOCKER_REGISTRY, "test", 3, "snapshot");

        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                null,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        final Map<String, String> labels = Map.of(
            "app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
            "app.kubernetes.io/component", "streamsApp"
        );

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                assertThat(deployment.getMetadata())
                    .hasFieldOrPropertyWithValue("name", DEFAULT_DEPLOYMENT_NAME);

                final DeploymentSpec deploymentSpec = deployment.getSpec();
                assertThat(deploymentSpec)
                    .hasFieldOrPropertyWithValue("replicas", 1)
                    .extracting(spec -> spec.getTemplate().getMetadata().getLabels(), MAP)
                    .contains(Map.entry("app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME));

                assertThat(deployment.getMetadata())
                    .satisfies(metadata -> {
                        assertThat(metadata.getName()).isEqualTo(DEFAULT_DEPLOYMENT_NAME);
                        assertThat(metadata.getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
                    });

                final PodSpec podSpec = deploymentSpec.getTemplate().getSpec();
                assertThat(podSpec.getImagePullSecrets()).isNullOrEmpty();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("image", imageConfig.asImageString())
                    .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
                    .hasFieldOrPropertyWithValue("name", DEFAULT_DEPLOYMENT_NAME)
                    .satisfies(container -> {
                        assertThat(container.getPorts()).isNullOrEmpty();
                        assertThat(container.getEnv())
                            .hasSize(1)
                            .first()
                            .extracting(EnvVar::getName)
                            .isEqualTo("JAVA_TOOL_OPTIONS");
                        assertThat(container.getEnvFrom())
                            .isNotNull()
                            .hasSize(1)
                            .first()
                            .extracting(EnvFromSource::getConfigMapRef)
                            .isNotNull()
                            .hasFieldOrPropertyWithValue("name", KubernetesResources.QUICK_CONFIG_NAME);

                        assertThat(container.getArgs())
                            .hasSize(2)
                            .contains("--brokers=bootstrap")
                            .contains("--schema-registry-url=http://dummy:123");
                    });
            });
    }

    @Test
    void shouldCreateAppDeploymentWithArgument() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                null,
                null,
                Map.of("--test", "my-value", "--foo", "bar"));

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .satisfies(container -> assertThat(container.getArgs())
                        .hasSize(4)
                        .contains("--brokers=bootstrap")
                        .contains("--schema-registry-url=http://dummy:123")
                        .contains("--test=my-value", "--foo=bar"));
            });
    }

    @Test
    void shouldCreateAppDeploymentWithReplica() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                3,
                null,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final DeploymentSpec deploymentSpec = deployment.getSpec();

                assertThat(deploymentSpec)
                    .hasFieldOrPropertyWithValue("replicas", 3)
                    .extracting(spec -> spec.getTemplate().getMetadata().getLabels(), MAP)
                    .contains(Map.entry("app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME));

            });
    }

    @Test
    void shouldCreateAppDeploymentWithDefaultReplica() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                null,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {

                final DeploymentSpec deploymentSpec = deployment.getSpec();

                assertThat(deploymentSpec).hasFieldOrPropertyWithValue("replicas", 1);

            });
    }

    @Test
    void shouldExportHostInfoForAppWithPort() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                DEFAULT_PORT,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                final List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
                assertThat(containers)
                    .isNotNull()
                    .first()
                    .satisfies(
                        container -> {
                            assertThat(container.getEnv().stream().map(EnvVar::getName)).containsAll(
                                List.of("POD_IP", "CONTAINER_PORT"));
                            assertThat(container.getEnv().stream().filter(env -> "CONTAINER_PORT".equals(env.getName()))
                                .findFirst().orElseThrow().getValue()).isEqualTo(String.valueOf(DEFAULT_PORT));
                        }
                    );
            });
    }

    @Test
    void shouldAnnotateAppHasService() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                1,
                DEFAULT_PORT,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(
                deployment -> assertThat(deployment.getMetadata().getAnnotations().get("d9p.io/has-service")).isEqualTo(
                    "true"));
    }

    @Test
    void shouldCreateServiceForPortOption() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                1,
                DEFAULT_PORT,
                    null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.SERVICE);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Service.class))
            .satisfies(service -> {
                assertThat(service.getMetadata().getName()).isEqualTo(DEFAULT_DEPLOYMENT_NAME);
                assertThat(service.getSpec().getSelector()).isEqualTo(
                    Map.of("app.kubernetes.io/name", DEFAULT_DEPLOYMENT_NAME,
                        "app.kubernetes.io/component", "streamsApp"));
                assertThat(service.getSpec().getPorts().get(0).getTargetPort()).isEqualTo(new IntOrString(
                    DEFAULT_PORT));
            });
    }

    @Test
    void shouldCreateServiceThatSelectsApp() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                DEFAULT_PORT,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.SERVICE);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Service.class))
            .satisfies(
                service -> {
                    final Map<String, String> serviceSelector = service.getSpec().getSelector();
                    final Optional<HasMetadata> resource = findResource(applicationResources, ResourceKind.DEPLOYMENT);
                    assertThat(resource)
                        .isPresent()
                        .get(InstanceOfAssertFactories.type(Deployment.class))
                        .satisfies(deployment -> assertThat(deployment.getMetadata().getLabels()).containsAllEntriesOf(
                            serviceSelector));
                }
            );
    }

    @Test
    void shouldSyncTargetPortAndContainerPort() {
        final ApplicationResources applicationResources =
            this.createApplicationResource(
                null,
                DEFAULT_PORT,
                null,
                Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(
                deployment -> {
                    final Container appContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
                    final Optional<HasMetadata> resource = findResource(applicationResources, ResourceKind.SERVICE);

                    assertThat(resource)
                        .isPresent()
                        .get(InstanceOfAssertFactories.type(Service.class))
                        .satisfies(
                            service -> assertThat(appContainer.getPorts()).anyMatch(
                                port -> new IntOrString(port.getContainerPort()).equals(
                                    service.getSpec().getPorts().get(0).getTargetPort())));
                });
    }

    @Test
    void shouldCreateAppDeploymentWithImagePullSecret() {
        final ApplicationResources applicationResources =
                this.createApplicationResource(
                        null,
                        null,
                        DEFAULT_SECRET,
                        Map.of());

        final Optional<HasMetadata> hasMetadata = findResource(applicationResources, ResourceKind.DEPLOYMENT);

        assertThat(hasMetadata)
                .isPresent()
                .get(InstanceOfAssertFactories.type(Deployment.class))
                .satisfies(deployment -> {
                    final List<LocalObjectReference> imagePullSecrets = deployment.getSpec().getTemplate().getSpec()
                            .getImagePullSecrets();
                    assertThat(imagePullSecrets).hasSize(1);
                    final String imagePullSecret = imagePullSecrets.get(0).getName();
                    assertThat(imagePullSecret).isEqualTo(DEFAULT_SECRET);
                });
    }

    private ApplicationResources createApplicationResource(@Nullable final Integer replicas,
        @Nullable final Integer port, @Nullable final String imagePullSecret,
        final Map<String, String> arguments) {

        final ApplicationCreationData appCreationData =
            new ApplicationCreationData(NAME, KubernetesTest.DOCKER_REGISTRY, "test",
                "snapshot", replicas, port, imagePullSecret, arguments);

        return this.loader.forCreation(appCreationData, ResourcePrefix.APPLICATION);
    }
}
