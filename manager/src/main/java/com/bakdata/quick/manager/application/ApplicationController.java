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
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Inject;

/**
 * REST API for controlling applications.
 */
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ApplicationController {
    private final ApplicationService service;

    @Inject
    public ApplicationController(final ApplicationService service) {
        this.service = service;
    }

    /**
     * Retrieves information about the application identified by its name.
     */
    @Get("/application/{name}")
    public Single<ApplicationDescription> getApplicationInformation(final String name) {
        return this.service.getApplicationInformation(name);
    }

    /**
     * Deploys a new Kafka Streams application in a Docker container.
     *
     * @param applicationCreationData {@link ApplicationCreationData}
     */
    @Post(value = "/application/", consumes = {MediaType.APPLICATION_JSON})
    public Completable deployApplication(final ApplicationCreationData applicationCreationData) {
        return this.service.deployApplication(applicationCreationData);
    }

    /**
     * Deletes an application and runs a cleanup job.
     */
    @Delete("/application/{name}")
    public Completable deleteApplication(final String name) {
        return this.service.deleteApplication(name);
    }
}
