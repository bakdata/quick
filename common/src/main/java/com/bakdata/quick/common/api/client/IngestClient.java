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

import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.KeyValuePair;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Completable;
import java.util.List;

/**
 * HTTP client for interacting with the ingest REST API.
 */
@Client(value = "http://${quick.service.ingest}", errorType = ErrorMessage.class)
public interface IngestClient {
    @Post("/{topic}")
    <K, V> Completable sendData(@PathVariable final String topic, @Body final List<KeyValuePair<K, V>> keyValuePairs);

    @Delete("/{topic}")
    <K> Completable deleteData(@PathVariable final String topic, @Body final List<K> key);

    @Delete("/{topic}/{key}")
    <K> Completable deleteDataSingle(@PathVariable final String topic, final K key);
}
