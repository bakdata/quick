/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bakdata.quick.gateway.subscriptions;


import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;

/**
 * Keeps the state of the web socket subscriptions for one connection.
 *
 * @author Gerard Klijs
 * @since 1.3
 */
@Singleton
@Slf4j
class GraphQLWsOperations {

    private final ConcurrentHashMap<String, Subscription> activeOperations = new ConcurrentHashMap<>();

    /**
     * Cancels all containing subscriptions.
     */
    void cancelAll() {
        log.debug("Cancel all operations");
        for (final Subscription subscription : this.activeOperations.values()) {
            subscription.cancel();
        }
    }

    /**
     * Add the subscription to the map, done with a function such that when it already exists a second subscription is
     * not started.
     *
     * @param operationId String
     * @param starter     function to start a subscription,
     */
    void addSubscription(final String operationId, final Function<String, Subscription> starter) {
        log.debug("Save operation {}", operationId);
        this.activeOperations.computeIfAbsent(operationId, starter);
    }

    /**
     * Cancels the operation if it exists.
     *
     * @param operationId String
     */
    void cancelOperation(final String operationId) {
        log.debug("Cancel operation {}", operationId);
        Optional.ofNullable(this.activeOperations.get(operationId)).ifPresent(Subscription::cancel);
    }

    /**
     * Remove the operation once completed, to clean up and prevent sending a second complete on stop.
     *
     * @param operationId String
     * @return whether the operation was removed
     */
    boolean removeCompleted(final String operationId) {
        log.debug("Remove completed operation {}", operationId);
        if (operationId != null) {
            return this.activeOperations.remove(operationId) != null;
        } else {
            return false;
        }
    }

    /**
     * Whether the operation currently already exists.
     *
     * @param request the {@link GraphQLWsRequest} instance
     * @return whether it exists or not
     */
    boolean operationExists(final GraphQLWsRequest request) {
        return Optional.ofNullable(request)
            .map(GraphQLWsRequest::getId)
            .map(id -> this.activeOperations.containsKey(id))
            .orElse(false);
    }
}
