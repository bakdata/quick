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

package com.bakdata.quick.common.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.security.config.SecurityConfigurationProperties;
import io.micronaut.security.filters.SecurityFilter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * HTTP Filter for setting the token attribute in the request with the api key allowing us to use Micronaut's token
 * propagation.
 */
@Requires(property = SecurityConfigurationProperties.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@Filter(Filter.MATCH_ALL_PATTERN)
@Slf4j

public class TokenPropagator extends OncePerRequestHttpServerFilter {
    private static final String API_KEY_HEADER_NAME = "X-API-Key";

    @Override
    protected Publisher<MutableHttpResponse<?>> doFilterOnce(final HttpRequest<?> request,
        final ServerFilterChain chain) {
        final Optional<String> header = request.getHeaders().findFirst(API_KEY_HEADER_NAME);
        header.ifPresent(authToken -> {
            log.trace("Propagate token for request {}", request.getPath());
            request.setAttribute(SecurityFilter.TOKEN, authToken);
        });
        return chain.proceed(request);
    }
}
