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
import okhttp3.ResponseBody;

/**
 * Responsible for managing requests send to the Mirror.
 */
public interface MirrorRequestManager {

    /**
     * Responsible for making a request to a specific url and processing the result.
     *
     * @param url    a url for which a request is made
     * @param parser parser
     * @param <T>    type
     *
     * @return the value from a mirror value wrapper
     */
    @Nullable
    <T> T sendRequest(final String url, final ParserFunction<T> parser);

    /**
     * Submits a request and processes the response. Throws an exception in case of various errors.
     *
     * @param url a url for which a request is made
     *
     * @return response body if successful; null if resource has not been found
     */
    @Nullable
    ResponseBody makeRequest(final String url);
}
