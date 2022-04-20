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

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import javax.inject.Singleton;
import org.reactivestreams.Publisher;

/**
 * AuthenticationFetcher for API-Key based auth.
 *
 * <p>
 * The fetcher looks up the header named 'X-API-Key' and compares it to the injected token for this deployment. If they
 * are equal, the request is authenticated with an {@link ApiKeyAuthentication}.
 *
 * <p>
 * <b>Note</b>: The returned authentication does not have any roles and thus only works for {@link
 * io.micronaut.security.rules.SecurityRule#IS_AUTHENTICATED}.
 *
 * @see io.micronaut.security.filters.SecurityFilter
 * @see ApiKeyAuthentication
 */
@Singleton
public class ApiKeyAuthenticationFetcher implements AuthenticationFetcher {
    private static final String API_KEY_HEADER_NAME = "X-API-Key";
    private final SecurityConfig securityConfig;

    public ApiKeyAuthenticationFetcher(final SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public Publisher<Authentication> fetchAuthentication(final HttpRequest<?> request) {
        final boolean matchingToken = request.getHeaders()
            .findFirst(API_KEY_HEADER_NAME)
            .map(token -> token.equals(this.securityConfig.getApiKey()))
            .orElse(false);

        if (matchingToken) {
            return Publishers.just(new ApiKeyAuthentication());
        } else {
            return Publishers.empty();
        }
    }
}
