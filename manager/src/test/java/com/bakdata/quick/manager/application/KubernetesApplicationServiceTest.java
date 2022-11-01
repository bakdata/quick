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

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.manager.application.resources.ApplicationResourceLoader;
import com.bakdata.quick.manager.k8s.ImageConfig;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.reactivex.Completable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KubernetesApplicationServiceTest extends KubernetesTest {
    private static final String APP_NAME = "my-app";
    private static final String DEPLOYMENT_NAME = ResourcePrefix.APPLICATION.getPrefix() + APP_NAME;
    private static final String IMAGE_NAME = "my-image";
    private static final int DEFAULT_PORT = 8080;
    public static final String REQUEST_ID = "request123";

    private final KafkaConfig kafkaConfig = new KafkaConfig("bootstrap", "http://dummy:123");
    private ApplicationService service = null;

    @BeforeEach
    void setUp() {
        final ApplicationResourceLoader loader =
            new ApplicationResourceLoader(new KubernetesResources(), this.kafkaConfig, this.getAppSpecConfig(),
                this.getDeploymentConfig());
        this.service = new KubernetesApplicationService(new KubernetesResources(),
            this.getManagerClient(), loader);
    }

    @Test
    void shouldCreateDeploymentForAppDefaults() {
        this.deployApplication(null, Map.of());

        final List<Deployment> items = this.getDeployments();

        assertThat(items)
            .isNotNull()
            .hasSize(1);

        final Deployment deployment = items.get(0);

        assertThat(deployment.getMetadata())
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);
    }

    @Test
    void shouldCreateServiceForPortOption() {
        this.deployApplication(DEFAULT_PORT, Map.of());

        final List<Service> services = this.getServices();
        assertThat(services)
            .isNotNull()
            .first()
            .satisfies(service -> assertThat(service.getMetadata().getName()).isEqualTo(DEPLOYMENT_NAME));
    }

    @Test
    void shouldNotCreateServiceWithoutPort() {
        this.deployApplication(null, Map.of());
        final List<Service> services = this.getServices();
        assertThat(services).isNullOrEmpty();
    }

    @Test
    void shouldDeleteDeployment() {
        this.deployApplication(null, Map.of());
        assertThat(this.getDeployments()).isNotNull().hasSize(1);
        final Completable deleteRequest = this.service.deleteApplication(APP_NAME, REQUEST_ID);
        Optional.ofNullable(deleteRequest.blockingGet()).ifPresent(Assertions::fail);
        assertThat(this.getDeployments()).isNullOrEmpty();
    }

    @Test
    void shouldDeleteAppServiceWithApp() {
        this.deployApplication(DEFAULT_PORT, Map.of());
        assertThat(this.getServices()).isNotNull().hasSize(1).first().satisfies(
            service -> assertThat(service.getMetadata().getName()).isEqualTo(DEPLOYMENT_NAME)
        );
        final Completable deleteRequest = this.service.deleteApplication(APP_NAME, REQUEST_ID);
        Optional.ofNullable(deleteRequest.blockingGet()).ifPresent(Assertions::fail);
        assertThat(this.getServices()).isNullOrEmpty();
    }


    @Test
    void shouldCreateDeletionJob() {
        final ImageConfig imageConfig = ImageConfig.of(DOCKER_REGISTRY, IMAGE_NAME, 1, DEFAULT_IMAGE_TAG);

        this.deployApplication(DEFAULT_PORT, Map.of("--input-topics", "test"));
        assertThat(this.getDeployments()).isNotNull().hasSize(1);
        final Completable deleteRequest = this.service.deleteApplication(APP_NAME, REQUEST_ID);
        Optional.ofNullable(deleteRequest.blockingGet()).ifPresent(Assertions::fail);
        assertThat(this.client.batch().v1().jobs().list().getItems())
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(job -> assertThat(job.getMetadata().getName()).isEqualTo(DEPLOYMENT_NAME + "-deletion"))
            .extracting(job -> job.getSpec().getTemplate().getSpec().getContainers(),
                InstanceOfAssertFactories.list(Container.class))
            .hasSize(1)
            .first()
            .satisfies(container -> {
                assertThat(container.getImage()).isEqualTo(imageConfig.asImageString());
                assertThat(container.getArgs()).contains("--input-topics=test");
            });
    }

    @Test
    void shouldRejectDuplicateApplicationCreation() {
        final ApplicationCreationData applicationCreationData = new ApplicationCreationData(APP_NAME,
                DOCKER_REGISTRY,
                IMAGE_NAME,
                DEFAULT_IMAGE_TAG,
                1,
                DEFAULT_PORT,
                null,
                Map.of());

        final Completable firstDeployment = this.service.deployApplication(applicationCreationData, REQUEST_ID);
        Optional.ofNullable(firstDeployment.blockingGet()).ifPresent(Assertions::fail);
        final Throwable invalidDeployment = this.service.deployApplication(applicationCreationData, REQUEST_ID)
            .blockingGet();
        assertThat(invalidDeployment).isInstanceOf(BadArgumentException.class)
                .hasMessageContaining(String.format("The resource with the name %s already exists", APP_NAME));
    }

    private void deployApplication(@Nullable final Integer port, final Map<String, String> arguments) {
        final ApplicationCreationData applicationCreationData = new ApplicationCreationData(APP_NAME,
            DOCKER_REGISTRY,
            IMAGE_NAME,
            DEFAULT_IMAGE_TAG,
            1,
            port,
            null,
            arguments);

        final Completable completable = this.service.deployApplication(applicationCreationData, REQUEST_ID);

        Optional.ofNullable(completable.blockingGet()).ifPresent(Assertions::fail);
    }
}
