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

package com.bakdata.quick.common.exception.handler;

import com.bakdata.quick.common.api.model.ErrorMessage;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception handler for {@link HttpClientResponseException}.
 */
@Singleton
@Slf4j
public class HttpClientResponseExceptionHandler
    implements ExceptionHandler<HttpClientResponseException, HttpResponse<ErrorMessage>> {

    private final StatusHandler statusHandler;

    public HttpClientResponseExceptionHandler(final StatusHandler statusHandler) {
        this.statusHandler = statusHandler;
    }

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final HttpClientResponseException ex) {
        final Optional<ErrorMessage> errorMessage = ex.getResponse().getBody(ErrorMessage.class);
        // We expect that all services in quick respond with an error message
        if (errorMessage.isPresent()) {
            return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(errorMessage.get().getCode()))
                .body(errorMessage.get());
        }
        // we can use a fallback, but such cases should be fixed then
        log.warn("No error message found in for request {}", request, ex);
        final Optional<Object> body = ex.getResponse().getBody(Object.class);
        if (body.isPresent() && body.get() instanceof Exception) {
            return this.statusHandler.handle(request, ex.getStatus(), ((Throwable) body.get()).getMessage());
        }
        return this.statusHandler.handle(request, ex.getStatus(), "Error: " + ex.getStatus().getReason());
    }
}
