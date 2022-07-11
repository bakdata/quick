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

import com.bakdata.quick.common.api.model.mirror.MirrorValue;
import com.bakdata.quick.common.exception.MirrorException;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A default implementation of MirrorRequestManager.
 */
@Slf4j
public class DefaultMirrorRequestManager implements MirrorRequestManager {

    protected final HttpClient client;

    public DefaultMirrorRequestManager(final HttpClient client) {
        this.client = client;
    }

    @Override
    @Nullable
    public <T> T sendRequest(final String url, final ParserFunction<T> parser) {
        try {
            final ResponseBody body = makeRequest(url);
            final MirrorValue<T> mirrorValue;
            if (body != null) {
                mirrorValue = parser.parse(body.byteStream());
                return mirrorValue.getValue();
            }
            return null;
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }

    @Override
    @Nullable
    public ResponseBody makeRequest(final String url) {
        final Request request = new Request.Builder().url(url).get().build();
        try {
            final Response response = this.client.newCall(request).execute();
            if (response.code() == HttpStatus.NOT_FOUND.getCode()) {
                return null;
            }

            final ResponseBody body = response.body();
            if (response.code() != HttpStatus.OK.getCode()) {
                log.error("Got error response from mirror: {}", body);
                final String errorMessage =
                    String.format("Error while fetching data. Requested resource responded with status code %d",
                        response.code());
                throw new MirrorException(errorMessage, HttpStatus.valueOf(response.code()));
            }
            // Code 200 and empty body indicates an error
            if (body == null) {
                throw new MirrorException("Resource responded with empty body", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return body;
        } catch (final IOException exception) {
            throw new MirrorException("Not able to parse content", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
