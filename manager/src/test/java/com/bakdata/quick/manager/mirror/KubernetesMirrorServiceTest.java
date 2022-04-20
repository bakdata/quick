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


import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import com.bakdata.quick.manager.k8s.resource.QuickResources.ResourcePrefix;
import com.bakdata.quick.manager.mirror.resources.MirrorResourceLoader;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.reactivex.Completable;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KubernetesMirrorServiceTest extends KubernetesTest {
    private static final String TOPIC_NAME = "test-topic";
    private static final String DEPLOYMENT_NAME = ResourcePrefix.MIRROR.getPrefix() + TOPIC_NAME;
    private MirrorService mirrorService = null;

    @BeforeEach
    void setUp() {
        final MirrorResourceLoader loader =
            new MirrorResourceLoader(new KubernetesResources(),
                this.getDeploymentConfig(),
                this.getResourceConfig());

        this.mirrorService = new KubernetesMirrorService(new KubernetesResources(),
            this.getManagerClient(), this.getDeploymentConfig(), loader);
    }

    @Test
    void shouldCreateMirrorResources() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);

        final List<Deployment> deployments = this.getDeployments();
        final List<Service> services = this.getServices();

        assertThat(deployments)
            .isNotNull()
            .hasSize(1);

        assertThat(services)
            .isNotNull()
            .hasSize(1);
    }

    @Test
    void shouldCreateDeployment() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);

        final List<Deployment> items = this.getDeployments();

        assertThat(items)
            .isNotNull()
            .hasSize(1);

        final Deployment deployment = items.get(0);

        assertThat(deployment.getMetadata())
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);
    }

    @Test
    void shouldCreateServiceWithDefaults() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);

        final List<Service> services = this.getServices();

        assertThat(services)
            .isNotNull()
            .hasSize(1)
            .first()
            .extracting(Service::getMetadata)
            .hasFieldOrPropertyWithValue("name", DEPLOYMENT_NAME);
    }

    @Test
    void shouldDeleteDeployment() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);
        assertThat(this.getDeployments()).isNotNull().hasSize(1);

        final Completable deleteRequest = this.mirrorService.deleteMirror(TOPIC_NAME);
        Optional.ofNullable(deleteRequest.blockingGet()).ifPresent(Assertions::fail);

        assertThat(this.getDeployments()).isNullOrEmpty();
    }

    @Test
    void shouldDeleteService() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);
        assertThat(this.getServices()).isNotNull().hasSize(1);

        final Completable deleteRequest = this.mirrorService.deleteMirror(TOPIC_NAME);
        Optional.ofNullable(deleteRequest.blockingGet()).ifPresent(Assertions::fail);

        assertThat(this.getServices()).isNullOrEmpty();
    }

    @Test
    void shouldCreateDeletionJob() {
        final MirrorCreationData mirrorCreationData = new MirrorCreationData(
            TOPIC_NAME,
            TOPIC_NAME,
            1,
            null,
            null);
        this.createMirror(mirrorCreationData);
        assertThat(this.getServices()).isNotNull().hasSize(1);

        final Completable deleteRequest = this.mirrorService.deleteMirror(TOPIC_NAME);
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
            .satisfies(container -> assertThat(container.getArgs()).contains("--input-topics=" + TOPIC_NAME));
    }

    private void createMirror(final MirrorCreationData mirrorCreationData) {
        final Completable completable = this.mirrorService.createMirror(mirrorCreationData);
        Optional.ofNullable(completable.blockingGet()).ifPresent(Assertions::fail);
    }
}
