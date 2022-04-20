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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.bakdata.quick.manager.k8s.KubernetesResources;
import com.bakdata.quick.manager.k8s.KubernetesTest;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobCleanerTest extends KubernetesTest {

    @Test
    void shouldDeleteSucceededJobs() {
        final JobCleaner jobCleaner = new JobCleaner(this.client);
        final KubernetesResources resources = new KubernetesResources();
        final Job deletionJob = resources.createDeletionJob("test", "image", List.of("--key", "value"));
        final Job finalJob = new JobBuilder(deletionJob)
            .withNewStatus().withActive(0).withSucceeded(1).endStatus()
            .build();
        this.kubernetesServer.getClient().batch().v1().jobs().create(finalJob);

        List<Job> jobList = this.kubernetesServer.getClient().batch().v1().jobs().list().getItems();
        assertThat(jobList).hasSize(1);

        assertThatNoException().isThrownBy(jobCleaner::deleteJobs);

        jobList = this.kubernetesServer.getClient().batch().v1().jobs().list().getItems();
        assertThat(jobList).isEmpty();
    }

    @Test
    void shouldRunWithoutErrorForNoExistingJobs() {
        final JobCleaner jobCleaner = new JobCleaner(this.client);
        assertThatNoException().isThrownBy(jobCleaner::deleteJobs);
    }

    @Test
    void shouldRunWithoutErrorForJobsWithoutStatus() {
        final JobCleaner jobCleaner = new JobCleaner(this.client);
        final KubernetesResources resources = new KubernetesResources();
        final Job deletionJob = resources.createDeletionJob("test", "image", List.of("--key", "value"));
        this.kubernetesServer.getClient().batch().v1().jobs().create(deletionJob);
        assertThatNoException().isThrownBy(jobCleaner::deleteJobs);
    }

    @Test
    void shouldRunWithoutErrorForWithoutSucceeded() {
        final JobCleaner jobCleaner = new JobCleaner(this.client);
        final KubernetesResources resources = new KubernetesResources();
        final Job deletionJob = resources.createDeletionJob("test", "image", List.of("--key", "value"));
        final Job runningJob = new JobBuilder(deletionJob)
            .withNewStatus().withActive(0).withSucceeded(null).endStatus()
            .build();
        this.kubernetesServer.getClient().batch().v1().jobs().create(runningJob);
        assertThatNoException().isThrownBy(jobCleaner::deleteJobs);
    }
}
