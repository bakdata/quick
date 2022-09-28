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

package com.bakdata.quick.common.api.client.mirror;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Responsible for managing requests send to the Mirror.
 */
public interface MirrorRequestManager {

    /**
     * Submits a request and processes the response.
     *
     * @param url A URL for which a request is made
     * @return a response body if successful; null if resource has not been found
     */
    ResponseWrapper makeRequest(final String url);

    @Nullable
    <T> T processResponse(final ResponseWrapper responseWrapper, final ParserFunction<T> parser);
}
