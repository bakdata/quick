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

import static com.bakdata.quick.common.api.client.HeaderConstants.REQUEST_HEADER;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;


/**
 * This filter adds the X-Request-ID header and a unique identifier (the header's value) to each request.
 * Thanks to the unique identifiers, every request is easily distinguishable from the rest,
 * facilitating the debugging process.
 */
@Slf4j
@Filter(Filter.MATCH_ALL_PATTERN)
public class RequestHeaderFilter implements HttpServerFilter {

    private final Supplier<UUID> uuidSupplier;

    public RequestHeaderFilter(final Supplier<UUID> uuidSupplier) {
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(final HttpRequest<?> request, final ServerFilterChain chain) {
        final boolean hasRequestHeader = request.getHeaders().contains(REQUEST_HEADER);
        if (!hasRequestHeader) {
            final String id = this.getUuid();
            request.mutate().header(REQUEST_HEADER, id);
        }
        log.debug("The request at {} has the following X-Request-ID value: {}",
            request.getPath(), request.getHeaders().get(REQUEST_HEADER));
        return chain.proceed(request);
    }

    private String getUuid() {
        return this.uuidSupplier.get().toString();
    }
}
