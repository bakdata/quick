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

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.exception.MirrorException;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A default implementation of MirrorRequestManager.
 */
@Slf4j
public class DefaultMirrorRequestManager implements MirrorRequestManager {

    private final HttpClient client;

    public DefaultMirrorRequestManager(final HttpClient client) {
        this.client = client;
    }

    @Override
    public ResponseWrapper makeRequest(final HttpUrl url) {
        final Request request = new Request.Builder().url(url).get().build();
        // Do not close the response here because its content is read later (try-with-resources
        // implicitly closes the processed resource).
        try {
            final Response response = this.client.newCall(request).execute();
            return ResponseWrapper.fromResponse(response);
        } catch (final IllegalStateException | IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }

    @Nullable
    @Override
    public <T> T processResponse(final ResponseWrapper responseWrapper, final ParserFunction<T> parser) {
        try {
            if (responseWrapper.getResponseBody() != null) {
                return parser.parse(responseWrapper.getResponseBody().byteStream()).getValue();
            }
            return null;
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        } finally {
            // We are sure that the response is processed and can be closed
            if (responseWrapper.getResponseBody() != null) {
                log.debug("Closing the response body.");
                responseWrapper.getResponseBody().close();
            }
        }
    }
}
