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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;

/**
 * Client for interacting with the REST API of mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface MirrorClient<K, V> {

    /**
     * fetches the value of the given key from the mirror topic.
     *
     * @param key a key to be fetched
     * @return a list of values. If the requested mirror responds with a NOT_FOUND code the function returns null.
     */
    @Nullable
    V fetchValue(final K key);

    /**
     * fetches all the values of a mirror topic.
     *
     * @return returns a list of all values in a topic. null.
     */
    List<V> fetchAll();

    /**
     * fetches the values of a list of keys from the mirror topic.
     *
     * @param keys list of keys to be fetched
     * @return a list of values. If the requested mirror responds with a NOT_FOUND code the function returns null.
     */
    @Nullable
    List<V> fetchValues(final List<K> keys);

    /**
     * checks if a key exists in mirror topic.
     *
     * @return True/False if key exists in mirror topic
     */
    boolean exists(final K key);
}
