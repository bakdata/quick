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

import com.bakdata.quick.common.api.client.HeaderConstants;
import com.bakdata.quick.common.exception.MirrorException;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A wrapper for a response from a call to be made with the HttpClient.
 *
 * <p>
 * It consists of a response body extracted from the response and a boolean that indicates
 * whether the X-Cache-Update header has been set. This header signals the need to update
 * the mapping between partitions and mirror hosts.
 */
@Slf4j
@Value
public class ResponseWrapper {

    @Nullable
    ResponseBody responseBody;
    boolean updateCacheHeaderSet;

    private ResponseWrapper(@Nullable final ResponseBody responseBody, final boolean headerSet) {
        this.responseBody = responseBody;
        this.updateCacheHeaderSet = headerSet;
    }

    private ResponseWrapper(@Nullable final ResponseBody responseBody) {
        this(responseBody, false);
    }

    /**
     * Static factory method for creating instances of ResponseWrapper.
     *
     * @param response the response from the mirror
     * @return an instance of ResponseWrapper
     */
    public static ResponseWrapper fromResponse(final Response response) {

        if (response.code() == HttpStatus.NOT_FOUND.getCode()) {
            return new ResponseWrapper(null, isCacheMissHeaderSet(response));
        }
        final ResponseBody body = getAndCheckResponseBody(response);
        if (response.header(HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER) != null) {
            return new ResponseWrapper(body, isCacheMissHeaderSet(response));
        }
        return new ResponseWrapper(body);
    }

    /**
     * Has a similar functionality to {@link ResponseWrapper#fromResponse(Response)}. The difference is
     * that it creates a response from the fallback service. When the fallback service is called,
     * the mapping between partitions and hosts has to be checked. Thus, all instances of ResponseWrapper returned
     * from this function have the parameter headerSet equal true.
     *
     * @param fallbackResponse the response from the fallback service
     * @return an instance of ResponseWrapper with the headerSet argument set to true
     */
    public static ResponseWrapper fromFallbackResponse(final Response fallbackResponse) {
        if (fallbackResponse.code() == HttpStatus.NOT_FOUND.getCode()) {
            return new ResponseWrapper(null, true);
        }
        final ResponseBody body = getAndCheckResponseBody(fallbackResponse);
        return new ResponseWrapper(body, true);
    }

    /**
     * Extract the ResponseBody from the Response and checks its validity.
     *
     * @param response the response from a particular service (for example, Mirror or Fallback Service)
     * @return an instance of ResponseBody or an exception if it could not be retrieved
     */
    private static ResponseBody getAndCheckResponseBody(final Response response) {
        final ResponseBody body = response.body();
        if (response.code() != HttpStatus.OK.getCode()) {
            log.error("Got error response from mirror: {}", body);
            final String errorMessage =
                String.format("Error while fetching data. Requested resource responded with status code %d",
                    response.code());
            throw new MirrorException(errorMessage, HttpStatus.valueOf(response.code()));
        }
        if (body == null) {
            throw new MirrorException("Resource responded with empty body", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return body;
    }

    /**
     * Checks if the X-Cache-Update from {@link HeaderConstants} header has been set.
     *
     * @param response a response from the http call
     * @return a boolean that indicates whether the X-Cache-Update header has been set
     */
    private static boolean isCacheMissHeaderSet(final Response response) {
        return response.header(HeaderConstants.UPDATE_PARTITION_HOST_MAPPING_HEADER) != null;
    }
}
