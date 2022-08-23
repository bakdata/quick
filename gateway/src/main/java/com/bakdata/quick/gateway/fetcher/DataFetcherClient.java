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

package com.bakdata.quick.gateway.fetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching values from rest endpoints and converting them into generic containers.
 */
public interface DataFetcherClient<V> {
    TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {
    };
    TypeReference<List<Map<String, Object>>> LIST_TYPE_REFERENCE = new TypeReference<>() {
    };

    /**
     * Fetches a single value from the given id.
     *
     * <p>
     * Therefore, it is important to note that the resource should respond with a single JSON object. Otherwise, the
     * parsing will fail.
     *
     * @param id resource to fetch from
     * @return parsed json as Map
     */
    @Nullable
    V fetchResult(final String id);

    /**
     * Fetches a list of values from multiple ids.
     *
     * <p>
     * Therefore, it is important to note that each resource should respond with a single JSON object. Otherwise, the
     * parsing will fail.
     *
     * @param ids list of ids to fetch from
     * @return List of parsed json as Map
     */
    @Nullable
    List<V> fetchResults(final List<String> ids);

    /**
     * Fetches a list of values from a single id.
     *
     * <p>
     * Therefore, it is important to note that each resource should respond with an array with JSON objects. Otherwise,
     * the parsing will fail.
     *
     * @return List of parsed json as map
     */
    @Nullable
    List<V> fetchList();
}
