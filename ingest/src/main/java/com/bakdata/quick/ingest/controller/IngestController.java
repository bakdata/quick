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

package com.bakdata.quick.ingest.controller;

import com.bakdata.quick.common.api.model.KeyValuePair;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.ingest.service.IngestFilter;
import com.bakdata.quick.ingest.service.IngestFilter.IngestLists;
import com.bakdata.quick.ingest.service.IngestParser;
import com.bakdata.quick.ingest.service.IngestService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API of the ingest service.
 */
@Slf4j
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class IngestController {
    private final TopicTypeService typeService;
    private final IngestService ingestService;
    private final IngestParser parser;
    private final IngestFilter filter;

    /**
     * Injectable Constructor.
     */
    @Inject
    public IngestController(final TopicTypeService typeService, final IngestService ingestService,
        final IngestParser parser, final IngestFilter filter) {
        this.typeService = typeService;
        this.ingestService = ingestService;
        this.parser = parser;
        this.filter = filter;
    }

    /**
     * Ingests data into the topics.
     */
    @Post("/{topic}")
    public <K, V> Completable sendData(final String topic, @Body final String payload) {
        log.debug("Incoming request: Ingest payload for topic {}", topic);
        final Single<QuickTopicData<K, V>> topicInformation = this.typeService.getTopicData(topic);

        return topicInformation.onErrorResumeNext(throwable -> errorDoesNotExistError(topic))
            .flatMapCompletable(info -> this.convertIngestData(topic, payload, info));
    }

    /**
     * Deletes key from topic.
     */
    @Delete("/{topic}/{rawKey}")
    public <K, V> Completable deleteValue(final String topic, final String rawKey) {
        log.debug("Incoming request: Delete {} for topic {}", rawKey, topic);
        final Single<QuickTopicData<K, V>> topicInformation = this.typeService.getTopicData(topic);

        return topicInformation.onErrorResumeNext(throwable -> errorDoesNotExistError(topic))
            .map(info -> info.getKeyData().getResolver().fromString(rawKey))
            .map(List::of)
            .flatMapCompletable(key -> this.ingestService.deleteData(topic, key));
    }

    /**
     * Deletes key from topic.
     */
    @Delete("/{topic}")
    public <K, V> Completable deleteValueFromBody(final String topic, @Body final String rawKey) {
        log.debug("Incoming request: Delete {} for topic {}", rawKey, topic);
        final Single<QuickTopicData<K, V>> topicInformation = this.typeService.getTopicData(topic);

        return topicInformation.onErrorResumeNext(throwable -> errorDoesNotExistError(topic))
            .map(info -> this.parser.parseKeyData(rawKey, info))
            .flatMapCompletable(key -> this.ingestService.deleteData(topic, key));
    }

    /**
     * Converts already existing keys into an error.
     *
     * @param topic the topic to ingest into
     * @param pairs lists of existing and non existing keys respectively
     * @return successful completable if there are no existing keys otherwise an error
     */
    private static Completable createErrorsForExistingKeys(final String topic, final IngestLists<?, ?> pairs) {
        if (pairs.getExistingData().isEmpty()) {
            return Completable.complete();
        } else {
            final String existingKeys = pairs.getExistingData().stream()
                .map(KeyValuePair::getKey)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            final String errorMessage = String.format("The following keys already exist for immutable topic %s: %s",
                topic, existingKeys);
            return Completable.error(new BadArgumentException(errorMessage));
        }
    }

    private static <K, V> Single<QuickTopicData<K, V>> errorDoesNotExistError(final String topic) {
        return Single.error(new BadArgumentException(String.format("Topic %s does not exists.", topic)));
    }

    /**
     * Processes raw input data for ingesting.
     *
     * <p>
     * Two steps are necessary to ingest the raw payload:
     * <ul>
     *    <li> parse it
     *    <li> check for existing keys if the topic is immutable
     * </ul>
     *
     * <p>
     * This method handles both steps and then forwards all non-existing {@link KeyValuePair} to the
     * {@link IngestService}.
     * Further, it handles merging the error messages. The method returns an error for existing keys as well as possible
     * errors in the ingest service.
     *
     * @param topic   the topic to ingest to
     * @param payload the raw payload
     * @param data    the topic information for the topic
     * @param <K>     the key type of the topic
     * @param <V>     the value type of the topic
     * @return merged completable possibly containing errors from existing keys or the ingest service
     */
    private <K, V> Completable convertIngestData(final String topic, final String payload,
        final QuickTopicData<K, V> data) {

        final Single<List<KeyValuePair<K, V>>> list = Single.fromCallable(() ->
            this.parser.parseInputData(payload, data)
        );

        return list
            .flatMap(pairs -> this.filter.prepareIngest(data, pairs))
            .flatMapCompletable(pairs -> {
                final Completable existingError = createErrorsForExistingKeys(topic, pairs);
                final Completable ingest = this.ingestService.sendData(topic, pairs.getDataToIngest());
                return Completable.mergeArrayDelayError(existingError, ingest);
            });
    }
}
