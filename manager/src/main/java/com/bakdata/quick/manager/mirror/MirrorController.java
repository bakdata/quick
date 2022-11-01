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

import static com.bakdata.quick.common.api.client.HeaderConstants.REQUEST_HEADER;

import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import jakarta.inject.Inject;

/**
 * Manager's REST API for managing mirror applications.
 */

@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class MirrorController {
    private final MirrorService service;

    @Inject
    public MirrorController(final MirrorService service) {
        this.service = service;
    }

    /**
     * Creates a new mirror application for a topic.
     *
     * @param mirrorCreationData {@link MirrorCreationData}
     */
    @Post("/topic/mirror")
    Completable createMirror(@Body final MirrorCreationData mirrorCreationData, final HttpHeaders headers) {
        return this.service.createMirror(mirrorCreationData, headers.get(REQUEST_HEADER));
    }

    /**
     * Deletes a mirror application.
     *
     * @param name the topic's name
     */
    @Delete("/topic/{name}/mirror")
    public Completable deleteMirror(final String name, final HttpHeaders headers) {
        return this.service.deleteMirror(name, headers.get(REQUEST_HEADER)).subscribeOn(Schedulers.io());
    }
}
