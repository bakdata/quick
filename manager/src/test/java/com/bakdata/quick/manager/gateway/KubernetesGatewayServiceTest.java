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

package com.bakdata.quick.manager.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.client.GatewayClient;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.manager.gateway.resource.GatewayResourceLoader;
import com.bakdata.quick.manager.graphql.GraphQLToAvroConverter;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.middleware.MiddlewareList;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.reactivex.Completable;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KubernetesGatewayServiceTest extends KubernetesTest {
    private static final String GATEWAY_IMAGE = "quick-gateway";
    private static final String GATEWAY_NAME = "test-gateway";
    private static final String DEPLOYMENT_NAME = "quick-gateway-test-gateway";
    private static final int CONTAINER_PORT = 8081;

    private final GatewayClient gatewayClient = Mockito.mock(GatewayClient.class);
    private final GraphQLToAvroConverter graphQLToAvroConverter = new GraphQLToAvroConverter("test.avro");
    private GatewayService gatewayService = null;

    @BeforeEach
    void setUp() {
        final GatewayResourceLoader loader =
            new GatewayResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getResourceConfig(),
                NAMESPACE);

        this.gatewayService = new KubernetesGatewayService(
            this.getManagerClient(),
            this.gatewayClient,
            this.graphQLToAvroConverter,
            loader);
    }

    @Test
    void shouldGetGatewayList() {
        this.createGateway(GATEWAY_NAME, 1, null);
        this.createGateway(GATEWAY_NAME + "-2", 1, null);

        final List<GatewayDescription> gatewayDescriptionList =
            this.gatewayService.getGatewayList().blockingGet();

        assertThat(gatewayDescriptionList.size()).isEqualTo(2);
        assertThat(gatewayDescriptionList)
            .extracting(GatewayDescription::getName)
            .containsExactlyInAnyOrder(GATEWAY_NAME, GATEWAY_NAME + "-2");
    }

    @Test
    void shouldGetGateway() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final GatewayDescription gatewayDescription =
            this.gatewayService.getGateway(GATEWAY_NAME).blockingGet();
        assertThat(gatewayDescription.getName()).isEqualTo(GATEWAY_NAME);
        assertThat(gatewayDescription.getReplicas()).isEqualTo(1);
        assertThat(gatewayDescription.getTag()).isEqualTo("latest");
    }

    @Test
    void shouldCreateDeploymentWithDefaults() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final List<Deployment> items = this.getDeployments();
        assertThat(items)
            .isNotNull()
            .hasSize(1);

        final Deployment deployment = items.get(0);
        assertThat(deployment.getMetadata())
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);

        final DeploymentSpec deploymentSpec = deployment.getSpec();
        assertThat(deploymentSpec)
            .hasFieldOrPropertyWithValue("replicas", 1);

        final PodSpec podSpec = deploymentSpec.getTemplate().getSpec();
        assertThat(podSpec.getContainers())
            .isNotNull()
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("image", getImage(DOCKER_REGISTRY, GATEWAY_IMAGE, DEFAULT_IMAGE_TAG))
            .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME)
            .satisfies(container -> {
                assertThat(container.getPorts())
                    .isNotNull()
                    .hasSize(2)
                    // main port
                    .anyMatch(port -> port.getContainerPort() == KubernetesResources.CONTAINER_PORT)
                    // metrics port
                    .anyMatch(port -> port.getContainerPort() == CONTAINER_PORT);

                assertThat(container.getEnvFrom())
                    .isNotNull()
                    .hasSize(2)
                    .first()
                    .extracting(EnvFromSource::getConfigMapRef)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", KubernetesResources.QUICK_CONFIG_NAME);

                assertThat(container.getEnvFrom())
                    .isNotNull()
                    .hasSize(2)
                    .last()
                    .extracting(EnvFromSource::getSecretRef)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", KubernetesResources.QUICK_API_KEY_SECRET);

                assertThat(container.getVolumeMounts())
                    .hasSize(2)
                    .anySatisfy(mount -> assertThat(mount)
                        .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME + "-config-volume")
                        .hasFieldOrPropertyWithValue("mountPath", "/app/schema.graphql")
                        .hasFieldOrPropertyWithValue("subPath", "schema.graphql"));
            });
    }

    @Test
    void shouldCreateService() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final List<Service> services = this.getServices();

        assertThat(services).isNotNull().hasSize(1);
    }

    @Test
    void shouldCreateIngressWithDefaults() {
        this.createGateway(GATEWAY_NAME, 1, null);
        final List<Ingress> ingresses = this.getIngressItems();
        assertThat(ingresses)
            .isNotNull()
            .hasSize(1)
            .first()
            .extracting(Ingress::getMetadata)
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);
    }

    @Test
    void shouldCreateMiddleware() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final MiddlewareList middlewares = this.getMiddlewares();
        assertThat(middlewares.getItems())
            .hasSize(1)
            .first()
            .satisfies(middleware -> {
                assertThat(middleware.getMetadata().getName()).isEqualTo(DEPLOYMENT_NAME);
                assertThat(middleware.getSpec().getStripPrefix()).isNotNull();
                assertThat(middleware.getSpec().getStripPrefix().getPrefixes())
                    .containsExactly("/gateway/" + GATEWAY_NAME);
            });
    }

    @Test
    void shouldCreateConfigMap() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final List<ConfigMap> configMaps = this.getConfigMaps();
        assertThat(configMaps)
            .hasSize(1)
            .first()
            .satisfies(configMap -> assertThat(configMap.getMetadata().getName())
                .isEqualTo(DEPLOYMENT_NAME + "-config"));
    }

    @Test
    void shouldCreateConfigmapWithSchema() {
        final String schema = "type Query { test: Int }";
        this.createGateway(GATEWAY_NAME, 1, null, schema);

        final List<ConfigMap> configMaps = this.getConfigMaps();
        assertThat(configMaps)
            .hasSize(1)
            .first()
            .extracting(ConfigMap::getData, InstanceOfAssertFactories.map(String.class, String.class))
            .containsOnlyKeys("schema.graphql")
            .extractingByKey("schema.graphql", InstanceOfAssertFactories.STRING)
            .containsIgnoringWhitespaces(schema);
    }


    @Test
    void shouldUpdateConfigMap() {
        this.createGateway(GATEWAY_NAME, 1, null);

        final String graphQLSchema = "type Query { find: String }";
        Mockito.when(this.gatewayClient.updateSchema(GATEWAY_NAME, new SchemaData(graphQLSchema)))
            .thenReturn(Completable.complete());

        this.gatewayService.updateSchema(GATEWAY_NAME, graphQLSchema).blockingAwait();

        final List<ConfigMap> configMaps = this.getConfigMaps();
        assertThat(configMaps)
            .hasSize(1)
            .first()
            .satisfies(configMap -> assertThat(configMap.getMetadata().getName())
                .isEqualTo(DEPLOYMENT_NAME + "-config"))
            .satisfies(configMap -> assertThat(configMap.getData().get("schema.graphql")).isEqualTo(graphQLSchema));
    }

    @Test
    void shouldSetReplicas() {
        final int replicas = 5;
        this.createGateway(GATEWAY_NAME, replicas, null);

        final List<Deployment> items = this.getDeployments();
        assertThat(items)
            .isNotNull()
            .hasSize(1);
        final Deployment deployment = items.get(0);
        assertThat(deployment.getMetadata())
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);
        final DeploymentSpec deploymentSpec = deployment.getSpec();
        assertThat(deploymentSpec)
            .hasFieldOrPropertyWithValue("replicas", replicas);
    }

    @Test
    void shouldSetImageVersion() {
        final String name = "test-gateway";
        final String expectedName = "quick-gateway-test-gateway";
        final String customTag = "custom-tag";
        this.createGateway(name, 1, customTag);

        final List<Deployment> items = this.getDeployments();

        assertThat(items)
            .isNotNull()
            .hasSize(1);

        final Deployment deployment = items.get(0);

        assertThat(deployment.getMetadata())
            .hasFieldOrPropertyWithValue("name", expectedName);

        assertThat(items.get(0).getSpec().getTemplate().getSpec().getContainers())
            .isNotNull()
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("image", getImage(DOCKER_REGISTRY, GATEWAY_IMAGE, customTag));
    }

    @Test
    void shouldDeleteDeployment() {
        this.createGateway(GATEWAY_NAME, 1, null);
        assertThat(this.getDeployments()).isNotNull().hasSize(1);
        this.deleteGatewayResources();
        assertThat(this.getDeployments()).isNullOrEmpty();
    }

    @Test
    void shouldDeleteIngress() {
        this.createGateway(GATEWAY_NAME, 1, null);
        assertThat(this.getIngressItems()).isNotNull().hasSize(1);
        this.deleteGatewayResources();
        assertThat(this.getIngressItems()).isNullOrEmpty();
    }

    @Test
    void shouldDeleteService() {
        this.createGateway(GATEWAY_NAME, 1, null);
        assertThat(this.getServices()).isNotNull().hasSize(1);
        this.deleteGatewayResources();
        assertThat(this.getServices()).isNullOrEmpty();
    }

    @Test
    void shouldDeleteMiddleware() {
        this.createGateway(GATEWAY_NAME, 1, null);
        assertThat(this.getMiddlewares().getItems())
            .hasSize(1);
        this.deleteGatewayResources();
        assertThat(this.getMiddlewares().getItems())
            .isNullOrEmpty();
    }

    @Test
    void shouldDeleteConfigMap() {
        this.createGateway(GATEWAY_NAME, 1, null);
        assertThat(this.getConfigMaps()).isNotNull().hasSize(1);
        this.deleteGatewayResources();
        assertThat(this.getConfigMaps()).isNullOrEmpty();
    }

    @Test
    void shouldRejectDuplicateGatewayCreation() {
        final GatewayCreationData creationData = new GatewayCreationData(GATEWAY_NAME, 1, null, null);
        final Throwable firstDeployment = this.gatewayService.createGateway(creationData).blockingGet();
        assertThat(firstDeployment).isNull();
        final Throwable invalidDeployment = this.gatewayService.createGateway(creationData).blockingGet();
        assertThat(invalidDeployment).isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("The resource with the name test-gateway already exists");
    }


    private void deleteGatewayResources() {
        this.gatewayService.deleteGateway(GATEWAY_NAME).blockingAwait();
    }


    private void createGateway(final String gatewayName, final int replicas, @Nullable final String tag) {
        this.createGateway(gatewayName, replicas, tag, null);
    }

    private void createGateway(final String gatewayName, final int replicas, @Nullable final String tag,
                               @Nullable final String schema) {
        final GatewayCreationData creationData = new GatewayCreationData(gatewayName, replicas, tag, schema);
        final Throwable throwable =
            this.gatewayService.createGateway(creationData).blockingGet();
        Optional.ofNullable(throwable).ifPresent(Assertions::fail);
    }
}
