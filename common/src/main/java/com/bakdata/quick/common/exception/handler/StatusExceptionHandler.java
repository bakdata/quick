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
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.HttpStatusHandler;
import javax.inject.Singleton;

/**
 * Handles exception of type {@link io.micronaut.http.exceptions.HttpStatusException}.
 *
 * <p>
 * Compared to {@link HttpStatusHandler}, this uses Quick's standard {@link ErrorMessage}.
 *
 * @see HttpStatusHandler
 */
@Replaces(HttpStatusHandler.class)
@Produces
@Singleton
public class StatusExceptionHandler implements ExceptionHandler<HttpStatusException, HttpResponse<ErrorMessage>> {
    private final StatusHandler statusHandler;

    public StatusExceptionHandler(final StatusHandler statusHandler) {
        this.statusHandler = statusHandler;
    }

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final HttpStatusException exception) {
        return this.statusHandler.handle(request, exception.getStatus(), exception.getBody().toString());
    }
}
