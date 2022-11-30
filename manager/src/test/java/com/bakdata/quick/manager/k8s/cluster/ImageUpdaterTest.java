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

import static com.bakdata.quick.manager.TestUtil.createDefaultMirrorCreationData;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.client.gateway.GatewayClient;
import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.manager.config.DeploymentConfig;
import com.bakdata.quick.manager.gateway.GatewayService;
import com.bakdata.quick.manager.gateway.KubernetesGatewayService;
import com.bakdata.quick.manager.gateway.resource.GatewayResourceLoader;
import com.bakdata.quick.manager.gateway.resource.GatewayResources;
import com.bakdata.quick.manager.graphql.GraphQLToAvroConverter;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.mirror.KubernetesMirrorService;
import com.bakdata.quick.manager.mirror.MirrorService;
import com.bakdata.quick.manager.mirror.resources.MirrorResourceLoader;
import com.google.common.collect.Iterables;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.reactivex.Completable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ImageUpdaterTest extends KubernetesTest {

    private static final String NEW_TAG = "new-tag";
    private static final String FIXED_TAG = "fixed";
    private static final String MIRROR_IMAGE = "quick-mirror";

    @Test
    void shouldUpdateGateway() {
        final GatewayClient gatewayClient = Mockito.mock(GatewayClient.class);

        final GatewayResourceLoader loader =
            new GatewayResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig(),
                NAMESPACE);

        final GatewayService gatewayService = new KubernetesGatewayService(
            this.getManagerClient(),
            gatewayClient,
            new GraphQLToAvroConverter("test.avro"),
            loader
        );

        gatewayService.createGateway(new GatewayCreationData("gateway", 1, null, null)).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(
                String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, DEFAULT_IMAGE_TAG));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, NEW_TAG));
    }


    @Test
    void shouldUpdateMultipleGateways() {
        final GatewayClient gatewayClient = Mockito.mock(GatewayClient.class);

        final GatewayResourceLoader loader =
            new GatewayResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig(),
                NAMESPACE);

        final GatewayService gatewayService = new KubernetesGatewayService(
            this.getManagerClient(),
            gatewayClient,
            new GraphQLToAvroConverter("test.avro"),
            loader
        );

        Completable.mergeArray(
            gatewayService.createGateway(new GatewayCreationData("gateway", 1, null, null)),
            gatewayService.createGateway(new GatewayCreationData("gateway2", 1, null, null))
        ).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(2)
            .extracting(ImageUpdaterTest::getContainerImage)
            .allMatch(name -> name.equals(
                String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, DEFAULT_IMAGE_TAG)));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(2)
            .extracting(ImageUpdaterTest::getContainerImage)
            .allMatch(name -> name.equals(
                String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, NEW_TAG)));
    }

    @Test
    void shouldNotUpdateFixedGateway() {
        final GatewayClient gatewayClient = Mockito.mock(GatewayClient.class);

        final GatewayResourceLoader loader =
            new GatewayResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig(),
                NAMESPACE);

        final GatewayService gatewayService = new KubernetesGatewayService(
            this.getManagerClient(),
            gatewayClient,
            new GraphQLToAvroConverter("test.avro"),
            loader
        );

        gatewayService.createGateway(new GatewayCreationData("gateway", 1, FIXED_TAG, null)).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(
                String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, FIXED_TAG));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(String.format("%s/%s:%s", DOCKER_REGISTRY, GatewayResources.GATEWAY_IMAGE, FIXED_TAG));
    }


    @Test
    void shouldUpdateMirror() {
        final MirrorResourceLoader loader =
            new MirrorResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig());

        final MirrorService mirrorService = new KubernetesMirrorService(new KubernetesResources(),
            this.getManagerClient(), this.getDeploymentConfig(), loader);

        final MirrorCreationData mirrorCreationData = createDefaultMirrorCreationData("topic");

        mirrorService.createMirror(mirrorCreationData).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(
                String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, DEFAULT_IMAGE_TAG));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, NEW_TAG));
    }


    @Test
    void shouldUpdateMultipleMirrors() {
        final MirrorResourceLoader loader =
            new MirrorResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig());

        final MirrorService mirrorService = new KubernetesMirrorService(new KubernetesResources(),
            this.getManagerClient(), this.getDeploymentConfig(), loader);
        final MirrorCreationData mirrorCreationData1 = createDefaultMirrorCreationData("topic");
        mirrorService.createMirror(mirrorCreationData1).blockingAwait();

        final MirrorCreationData mirrorCreationData2 = createDefaultMirrorCreationData("topic2");
        mirrorService.createMirror(mirrorCreationData2).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(2)
            .extracting(ImageUpdaterTest::getContainerImage)
            .allMatch(name -> name.equals(
                String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, DEFAULT_IMAGE_TAG)));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(2)
            .extracting(ImageUpdaterTest::getContainerImage)
            .allMatch(name -> name.equals(
                String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, NEW_TAG)));
    }

    @Test
    void shouldNotUpdateFixedMirror() {
        final MirrorResourceLoader loader =
            new MirrorResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getAppSpecConfig());

        final MirrorService mirrorService = new KubernetesMirrorService(new KubernetesResources(),
            this.getManagerClient(), this.getDeploymentConfig(), loader);
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            "topic",
            "service",
            1,
            FIXED_TAG, // use default tag
            null);
        mirrorService.createMirror(mirrorCreationData).blockingAwait();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(
                String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, FIXED_TAG));

        final DeploymentConfig newDeploymentConfig = this.createNewDeploymentConfig();
        final ImageUpdater imageUpdater = new ImageUpdater(this.client, newDeploymentConfig);

        imageUpdater.updateManagedDeployments();

        assertThat(this.getDeployments())
            .hasSize(1)
            .first()
            .extracting(ImageUpdaterTest::getContainerImage)
            .isEqualTo(String.format("%s/%s:%s", DOCKER_REGISTRY, MIRROR_IMAGE, FIXED_TAG));
    }

    private static String getContainerImage(final Deployment deployment) {
        return Iterables.getOnlyElement(deployment.getSpec().getTemplate().getSpec().getContainers()).getImage();
    }

    private DeploymentConfig createNewDeploymentConfig() {
        return new DeploymentConfig(this.getDeploymentConfig().getDockerRegistry(), NEW_TAG,
            this.getDeploymentConfig().getDefaultReplicas(), this.getDeploymentConfig().getIngressHost(),
            this.getDeploymentConfig().isIngressSsl(),
            this.getDeploymentConfig().getIngressEntrypoint());
    }
}
