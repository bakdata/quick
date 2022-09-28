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

import static com.bakdata.quick.common.api.client.ClientUtils.createMirrorUrlFromRequest;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.api.model.mirror.MirrorHost;
import com.bakdata.quick.common.exception.MirrorException;
import io.micronaut.http.HttpStatus;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of MirrorRequestManager that handles the case in which a replica is removed and the corresponding
 * host is no longer available. If this occurs, the fallback service is called.
 */
@Slf4j
public class MirrorRequestManagerWithFallback implements MirrorRequestManager {

    private final HttpClient client;
    private final MirrorHost fallbackServiceHost;
    private final MirrorRequestManager delegate;

    /**
     * Default constructor.
     *
     * @param client a http client
     * @param fallbackServiceHost the fallback service that is called when the desired replica is unavailable
     */
    public MirrorRequestManagerWithFallback(final HttpClient client, final MirrorHost fallbackServiceHost) {
        this.client = client;
        this.fallbackServiceHost = fallbackServiceHost;
        this.delegate = new DefaultMirrorRequestManager(client);
    }

    @Override
    public ResponseWrapper makeRequest(final String url) {
        final Request request = new Request.Builder().url(url).get().build();
        // Do not close the response here because its content is read later (try-with-resources
        // implicitly closes the processed resource).
        try {
            final Response response = this.client.newCall(request).execute();
            return ResponseWrapper.fromResponse(response);
        } catch (final IOException exception) {
            return this.getResponseFromFallbackService(request);
        }
    }

    @Nullable
    @Override
    public <T> T processResponse(final ResponseWrapper responseWrapper, final ParserFunction<T> parser) {
        return this.delegate.processResponse(responseWrapper, parser);
    }

    /**
     * This code covers a situation where a replica is removed, and we can no longer reach the host for a given
     * partition. The k8s-service (Load Balancer) is used as a fallback in such a case. It might also happen that the
     * Load Balancer itself is not reachable. If this occurs, we throw an exception. We also set the X-Cache-Update
     * header {@link HeaderConstants} to a response to immediately update the mapping.
     *
     * @param initialRequest the original request
     * @return an instance of ResponseWrapper with headerSet equals true
     */
    private ResponseWrapper getResponseFromFallbackService(final Request initialRequest) {
        log.info("Host at {} is unavailable.", initialRequest.url());
        final String newUrl = createMirrorUrlFromRequest(initialRequest, this.fallbackServiceHost);
        log.info("Forwarding the request to {}", newUrl);
        final Request fallbackRequest = new Request.Builder().url(newUrl).get().build();
        try {
            final Response fallbackResponse = this.client.newCall(fallbackRequest).execute();
            return ResponseWrapper.fromFallbackResponse(fallbackResponse);
        } catch (final IOException fallbackException) {
            throw new MirrorException("Unable to process the request. Load Balancer is not available.",
                HttpStatus.INTERNAL_SERVER_ERROR, fallbackException);
        }
    }
}
