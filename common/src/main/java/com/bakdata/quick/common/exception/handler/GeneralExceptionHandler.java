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
import com.bakdata.quick.common.api.model.HttpStatusError;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

/**
 * A fallback exception handler that will be used if there is no other handler is in place. Ideally, this class is never
 * called.
 */
@Produces
@Singleton
public class GeneralExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorMessage>> {

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final Exception exception) {
        final String detail = "An unexpected error occurred:" + exception.getMessage();
        final ErrorMessage error = HttpStatusError.toError(HttpStatus.INTERNAL_SERVER_ERROR, request.getPath(), detail);
        return HttpResponse.<ErrorMessage>serverError().body(error);
    }
}
