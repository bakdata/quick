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

import com.bakdata.quick.common.api.model.manager.ApplicationDescription;
import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Service managing application deployed in Quick.
 */
public interface ApplicationService {

    /**
     * Retrieves information about the application identified by its name.
     */
    Single<ApplicationDescription> getApplicationInformation(final String name);

    /**
     * Deletes an application and runs a clean up job.
     */
    Completable deleteApplication(final String name);

    /**
     * Deploys a new Kafka Streams application in a Docker container.
     *
     * @param applicationCreationData {@link ApplicationCreationData}
     */
    Completable deployApplication(final ApplicationCreationData applicationCreationData);
}
