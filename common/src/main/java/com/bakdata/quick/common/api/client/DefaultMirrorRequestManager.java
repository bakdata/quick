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

import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.exception.MirrorException;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A default implementation of MirrorRequestManager.
 */
@Slf4j
public class DefaultMirrorRequestManager implements MirrorRequestManager {

    private final HttpClient client;
    private final String fallbackServiceHost;

    public DefaultMirrorRequestManager(final HttpClient client, final MirrorHost fallbackServiceHost) {
        this.client = client;
        this.fallbackServiceHost = fallbackServiceHost.plainUrl();
    }

    @Override
    public ResponseWrapper makeRequest(final String url) {
        final Request request = new Request.Builder().url(url).get().build();
        /*
        The classic try-catch statement and not the try-with-resources is used here because the try-with-resources
        implicitly closes the processed resource in an automatically added (and not visible) block.
        If we used it here, we wouldn't be able to read from the InputStream in the caller
        as it would have been already closed by the try-with-resources in the callee.
         */
        try {
            final Response response = this.client.newCall(request).execute();
            return ResponseWrapper.fromResponse(response);
        } catch (final IOException exception) {
            /*
             * This code covers a situation where a replica is removed, and we can no longer
             * reach the host for a given partition. The k8s-service (Load Balancer) is used
             * as a fallback in such a case. It might also happen that the Load Balancer itself is not reachable.
             * If this happens, we throw an exception. We also set the X-Cache-Update header to a response
             * to immediately update the mapping.
             */
            if (exception instanceof UnknownHostException) {
                final String keyInfo = Arrays.stream(url.split("/")).skip(3).collect(Collectors.joining("/"));
                log.info("Host at {} is unavailable. Forwarding the request to {}", url,
                    fallbackServiceHost);
                final String newUrl = this.fallbackServiceHost.concat("/".concat(keyInfo));
                final Request fallbackRequest = new Request.Builder().url(newUrl).get().build();
                try {
                    final Response fallbackResponse = this.client.newCall(fallbackRequest).execute();
                    fallbackResponse.header(HeaderConstants.getCacheMissHeaderName(),
                        HeaderConstants.getCacheMissHeaderValue());
                    return ResponseWrapper.fromResponse(fallbackResponse);
                } catch (final IOException fallbackException) {
                    throw new MirrorException("Fallback service error", HttpStatus.INTERNAL_SERVER_ERROR,
                        fallbackException);
                }
            }
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
        }
    }
}
