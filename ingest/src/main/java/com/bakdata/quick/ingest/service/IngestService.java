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

package com.bakdata.quick.ingest.service;

import com.bakdata.quick.common.api.model.KeyValuePair;
import io.reactivex.Completable;
import java.util.List;

/**
 * Service for sending data into Kafka topics.
 */
public interface IngestService {

    /**
     * Ingests data into topic.
     *
     * @param topic         name of the topic
     * @param keyValuePairs list of key value pairs
     * @param <K>           type of the key
     * @param <V>           type of the value
     */
    <K, V> Completable sendData(final String topic, final List<KeyValuePair<K, V>> keyValuePairs);

    /**
     * Deletes keys from the topic.
     *
     * @param topic name of the topic
     * @param key   list of keys
     * @param <K>   type of the key
     */
    <K> Completable deleteData(final String topic, final List<K> key);
}
