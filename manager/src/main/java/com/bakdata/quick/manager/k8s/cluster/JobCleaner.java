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

import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Cleaner deleting succeeded k8s jobs periodically.
 */
@Singleton
@Slf4j
public class JobCleaner {
    private static final String NAME_LABEL_SELECTOR = "app.kubernetes.io/name";
    private final KubernetesClient client;

    public JobCleaner(final KubernetesClient client) {
        this.client = client;
    }

    /**
     * Queries the k8s api and deletes jobs that succeeded.
     *
     * <p>
     * This is required because k8s does not delete succeeded job automatically. You can set a key called 'ttl' (for
     * time to live) but this is supported only with a respective feature gate enabled.
     */
    @Scheduled(fixedRate = "30s")
    void deleteJobs() {
        try {
            final String[] labelsToDelete = this.client.batch().v1().jobs().list().getItems().stream()
                // Status and Succeeded might be null
                .filter(job -> Optional.ofNullable(job.getStatus())
                    .map(JobStatus::getSucceeded)
                    .map(succeeded -> succeeded == 1)
                    .orElse(false))
                .map(job -> job.getMetadata().getLabels().get(NAME_LABEL_SELECTOR))
                .toArray(String[]::new);

            if (labelsToDelete.length == 0) {
                return;
            }
            log.info("Delete jobs with names: {}", (Object) labelsToDelete);
            this.client.batch().v1().jobs().withLabelIn(NAME_LABEL_SELECTOR, labelsToDelete).delete();
        } catch (final KubernetesClientException e) {
            log.warn("Could not delete succeeded jobs", e);
        }
    }
}
