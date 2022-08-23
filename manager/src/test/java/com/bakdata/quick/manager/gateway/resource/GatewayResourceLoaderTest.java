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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.manager.TestUtil;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.middleware.Middleware;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressSpec;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayResourceLoaderTest extends KubernetesTest {
    private static final String EXPECTED_GATEWAY_IMAGE = "quick-gateway";
    private static final String GATEWAY_NAME = "gateway-name";
    private static final int REPLICAS = 3;
    private static final String TAG = "snapshot";

    private GatewayResourceLoader loader = null;

    @BeforeEach
    void setUp() {
        this.loader = new GatewayResourceLoader(new KubernetesResources(), this.getDeploymentConfig(),
            TestUtil.newResourceConfig(), NAMESPACE);
    }

    @Test
    void shouldCreateGatewayDeployment() {
        final GatewayCreationData gatewayCreationData = new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.DEPLOYMENT);

        final String deploymentName = ResourcePrefix.GATEWAY.getPrefix() + GATEWAY_NAME;

        final Map<String, String> labels = Map.of(
            "app.kubernetes.io/component", "gateway",
            "app.kubernetes.io/managed-by", "quick",
            "app.kubernetes.io/name", deploymentName
        );

        final Map<String, String> selectors = Map.of(
            "app.kubernetes.io/component", "gateway",
            "app.kubernetes.io/name", deploymentName
        );

        final Map<String, String> annotations = Map.of(
            "d9p.io/fixed-tag", "true"
        );

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .satisfies(deployment -> {
                final PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();
                assertThat(podSpec.getContainers())
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("image", getImage(EXPECTED_GATEWAY_IMAGE, TAG));

                assertThat(deployment.getMetadata())
                    .satisfies(metadata -> {
                        assertThat(metadata.getName()).isEqualTo(deploymentName);
                        assertThat(metadata.getLabels()).containsExactlyInAnyOrderEntriesOf(labels);
                        assertThat(metadata.getAnnotations()).containsExactlyInAnyOrderEntriesOf(annotations);
                    });

                assertThat(deployment.getSpec())
                    .satisfies(spec -> {
                        assertThat(spec.getReplicas()).isEqualTo(REPLICAS);
                        assertThat(spec.getSelector().getMatchLabels()).containsExactlyInAnyOrderEntriesOf(selectors);
                        assertThat(spec.getTemplate().getMetadata().getLabels()).containsExactlyInAnyOrderEntriesOf(
                            labels);
                    });

                assertThat(deployment.getSpec().getTemplate().getSpec().getVolumes())
                    .hasSize(2)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "log4j-config")
                    .extracting(Volume::getConfigMap)
                    .satisfies(configmap -> {
                        assertThat(configmap.getName()).isEqualTo("quick-log4j");
                        assertThat(configmap.getItems())
                            .hasSize(1)
                            .first()
                            .hasFieldOrPropertyWithValue("key", "log4j2.yaml")
                            .hasFieldOrPropertyWithValue("path", "log4j2.yaml");
                    });

                assertThat(deployment.getSpec().getTemplate().getSpec().getVolumes())
                    .hasSize(2)
                    .element(1)
                    .hasFieldOrPropertyWithValue("name", String.format("%s-config-volume", deploymentName))
                    .extracting(Volume::getConfigMap)
                    .satisfies(configmap -> assertThat(configmap.getName()).isEqualTo(
                        String.format("%s-config", deploymentName)));

            });
    }

    @Test
    void shouldCreateGatewayService() {
        final GatewayCreationData gatewayCreationData = new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.SERVICE);

        final String deploymentName = ResourcePrefix.GATEWAY.getPrefix() + GATEWAY_NAME;

        final Map<String, String> labels = Map.of(
            "app.kubernetes.io/name", deploymentName,
            "app.kubernetes.io/managed-by", "quick",
            "app.kubernetes.io/component", "gateway"
        );

        final Map<String, String> selectors = Map.of(
            "app.kubernetes.io/name", deploymentName,
            "app.kubernetes.io/component", "gateway"
        );

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Service.class))
            .satisfies(service -> {
                assertThat(service.getMetadata())
                    .hasFieldOrPropertyWithValue("name", deploymentName)
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
    void shouldCreateGatewayIngress() {
        final GatewayCreationData gatewayCreationData = new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.INGRESS);

        final String expected = ResourcePrefix.GATEWAY.getPrefix() + GATEWAY_NAME;

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Ingress.class))
            .satisfies(gatewayIngress -> {
                assertThat(gatewayIngress.getMetadata())
                    .hasFieldOrPropertyWithValue("name", expected)
                    .extracting(ObjectMeta::getAnnotations, MAP)
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "kubernetes.io/ingress.class", "traefik",
                        "traefik.ingress.kubernetes.io/router.middlewares",
                        String.format("%s-%s@kubernetescrd", "test", expected),
                        "traefik.ingress.kubernetes.io/router.entrypoints", "websecure",
                        "traefik.ingress.kubernetes.io/router.tls", "true"
                    ));

                final IngressSpec ingressSpec = gatewayIngress.getSpec();
                assertThat(ingressSpec).isNotNull();

                assertThat(ingressSpec.getRules()).isNotNull()
                    .hasSize(1)
                    .first()
                    .satisfies(rule -> assertThat(rule.getHost()).isEqualTo("quick.host.io"))
                    .extracting(IngressRule::getHttp)
                    .isNotNull()
                    .extracting(HTTPIngressRuleValue::getPaths, InstanceOfAssertFactories.list(HTTPIngressPath.class))
                    .isNotNull()
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("path", "/gateway/" + GATEWAY_NAME)
                    .hasFieldOrPropertyWithValue("pathType", "Prefix")
                    .extracting(path -> path.getBackend().getService())
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", expected)
                    .extracting(IngressServiceBackend::getPort)
                    .hasFieldOrPropertyWithValue("number", KubernetesResources.SERVICE_PORT);
            });
    }

    @Test
    void shouldCreateGatewayIngressWithHttpIngress() {
        // given null host and ingress point
        final Optional<String> host = Optional.empty();
        final String ingressEntrypoint = "web";
        final boolean useSsl = false;

        final GatewayCreationData gatewayCreationData = new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final DeploymentConfig customConfig = new DeploymentConfig(this.getDeploymentConfig().getDockerRegistry(),
            this.getDeploymentConfig().getDefaultImageTag(), this.getDeploymentConfig().getDefaultReplicas(), host,
            useSsl, ingressEntrypoint);

        final GatewayResourceLoader customLoader = new GatewayResourceLoader(new KubernetesResources(),
            customConfig, TestUtil.newResourceConfig(), NAMESPACE);
        final GatewayResources gatewayResources =
            customLoader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.INGRESS);

        final String expected = ResourcePrefix.GATEWAY.getPrefix() + GATEWAY_NAME;

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Ingress.class))
            .satisfies(gatewayIngress -> {
                assertThat(gatewayIngress.getMetadata())
                    .hasFieldOrPropertyWithValue("name", expected)
                    .extracting(ObjectMeta::getAnnotations, MAP)
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "kubernetes.io/ingress.class", "traefik",
                        "traefik.ingress.kubernetes.io/router.middlewares",
                        String.format("%s-%s@kubernetescrd", "test", expected),
                        "traefik.ingress.kubernetes.io/router.entrypoints", "web",
                        "traefik.ingress.kubernetes.io/router.tls", "false"
                    ));

                final IngressSpec ingressSpec = gatewayIngress.getSpec();
                assertThat(ingressSpec).isNotNull();

                assertThat(ingressSpec.getRules()).isNotNull()
                    .hasSize(1)
                    .first()
                    .satisfies(rule -> assertThat(rule.getHost()).isNull());
            });
    }

    @Test
    void shouldCreateGatewayConfigMap() {
        final GatewayCreationData gatewayCreationData =
            new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.CONFIGMAP);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(ConfigMap.class))
            .satisfies(configMap -> assertThat(configMap)
                .extracting(ConfigMap::getData, InstanceOfAssertFactories.map(String.class, String.class))
                .containsOnlyKeys("schema.graphql"));
    }

    @Test
    void shouldCreateGatewayConfigMapWithSchema() {
        final GatewayCreationData gatewayCreationData =
            new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, "something");
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);
        final Optional<HasMetadata> hasMetadata =
            findResource(gatewayResources, ResourceKind.CONFIGMAP);

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(ConfigMap.class))
            .satisfies(configMap -> assertThat(configMap)
                .extracting(ConfigMap::getData, InstanceOfAssertFactories.map(String.class, String.class))
                .containsOnlyKeys("schema.graphql")
                .extractingByKey("schema.graphql", InstanceOfAssertFactories.STRING)
                .containsIgnoringWhitespaces("something"));
    }

    @Test
    void shouldCreateGatewayMiddleware() {
        final GatewayCreationData gatewayCreationData = new GatewayCreationData(GATEWAY_NAME, REPLICAS, TAG, null);
        final GatewayResources gatewayResources =
            this.loader.forCreation(gatewayCreationData, ResourcePrefix.GATEWAY);

        final Optional<HasMetadata> hasMetadata = findResource(gatewayResources, ResourceKind.MIDDLEWARE);

        final String expected = ResourcePrefix.GATEWAY.getPrefix() + GATEWAY_NAME;

        assertThat(hasMetadata)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Middleware.class))
            .satisfies(middleware -> {
                assertThat(middleware.getMetadata().getName()).isEqualTo(expected);
                assertThat(middleware.getSpec().getStripPrefix().getPrefixes())
                    .containsExactly("/gateway/" + GATEWAY_NAME);
            });
    }
}
