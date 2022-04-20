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

package com.bakdata.quick.common.api.client;

import com.bakdata.quick.common.api.model.manager.ApplicationDescription;
import com.bakdata.quick.common.api.model.manager.creation.ApplicationCreationData;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Client for manager's application REST API.
 */
@Client("/")
public interface ApplicationClient {
    @Get("/application/{name}")
    Single<ApplicationDescription> getApplicationInformation(final String name);

    @Post(value = "/application/", consumes = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_YAML})
    Completable deployApplication(final ApplicationCreationData applicationCreationData);

    @Delete("/application/{name}")
    Completable deleteApplication(String name);
}
