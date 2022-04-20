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

package com.bakdata.quick.manager.k8s.resource;

import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.exception.InternalErrorException;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.reactivex.Completable;

/**
 * Resource error handler for k8s environments.
 */
public class KubernetesErrorHandler implements QuickResourceErrorHandler {
    private final String name;
    private final String resourceKind;

    /**
     * Default constructor.
     *
     * @param name         the name of this resource instance
     * @param resourceKind the name of the resource's kind
     */
    public KubernetesErrorHandler(final String name, final String resourceKind) {
        this.name = name;
        this.resourceKind = resourceKind;
    }

    @Override
    public Completable handleCreationError(final Throwable ex) {
        return createError(ex, String.format("%s %s could not be deployed.", this.resourceKind, this.name));
    }

    @Override
    public Completable handleDeletionError(final Throwable ex) {
        return createError(ex, String.format("%s %s could not be deleted.", this.resourceKind, this.name));
    }

    private static Completable createError(final Throwable ex, final String errorMessage) {
        if (ex instanceof KubernetesClientException) {
            final String reason = ((KubernetesClientException) ex).getStatus().getMessage();
            return Completable.error(new BadArgumentException(reason));
        }
        return Completable.error(new InternalErrorException(errorMessage));
    }
}
