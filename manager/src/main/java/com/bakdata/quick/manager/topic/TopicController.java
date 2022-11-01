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

package com.bakdata.quick.manager.topic;

import static com.bakdata.quick.common.api.client.HeaderConstants.REQUEST_HEADER;

import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.manager.creation.TopicCreationData;
import com.bakdata.quick.common.type.QuickTopicType;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Manager's REST API for topics.
 */
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
@Slf4j
public class TopicController {
    private final TopicService topicService;

    @Inject
    public TopicController(final TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * Retrieves a list of all topics.
     */
    @Get("/topics")
    public Single<List<TopicData>> getTopicList(final HttpHeaders headers) {
        return this.topicService.getTopicList(headers.get(REQUEST_HEADER));
    }

    /**
     * Retrieves information about a single topic.
     */
    @Get("/topic/{name}")
    public Single<TopicData> getTopicData(final String name, final HttpHeaders headers) {
        return this.topicService.getTopicData(name, headers.get(REQUEST_HEADER));
    }

    /**
     * Creates a new topic.
     *
     * @param name              name of the topic to create
     * @param keyType           topic's key type
     * @param valueType         topic's value type
     * @param topicCreationData additional data of the topic to create
     */
    @Post("/topic/{name}")
    public Completable createTopic(@PathVariable final String name,
                                   @QueryValue(defaultValue = "LONG") final QuickTopicType keyType,
                                   @QueryValue(defaultValue = "SCHEMA") final QuickTopicType valueType,
                                   @Body final TopicCreationData topicCreationData, final HttpHeaders headers) {
        return this.topicService.createTopic(name, keyType, valueType, topicCreationData,
                headers.get(REQUEST_HEADER))
            .doOnError(e -> log.error("Request {}: Could not create topic", headers.get(REQUEST_HEADER), e))
            .subscribeOn(Schedulers.io());
    }

    /**
     * Deletes an existing topic.
     *
     * @param name topic to delete
     */
    @Delete("/topic/{name}")
    public Completable deleteTopic(@PathVariable final String name, final HttpHeaders headers) {
        return this.topicService.deleteTopic(name, headers.get(REQUEST_HEADER)).subscribeOn(Schedulers.io());
    }
}
