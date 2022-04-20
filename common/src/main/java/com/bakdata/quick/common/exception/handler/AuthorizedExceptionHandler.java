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
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.authentication.DefaultAuthorizationExceptionHandler;
import javax.inject.Singleton;

/**
 * Defines behavior when AuthorizationException is thrown.
 *
 * @see DefaultAuthorizationExceptionHandler
 * @see AuthorizationException
 */
@Singleton
@Replaces(DefaultAuthorizationExceptionHandler.class)
public class AuthorizedExceptionHandler
    implements ExceptionHandler<AuthorizationException, HttpResponse<ErrorMessage>> {
    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final AuthorizationException exception) {
        final ErrorMessage error;
        if (exception.isForbidden()) {
            error = HttpStatusError
                .toError(HttpStatus.FORBIDDEN, request.getPath(), "You are not allowed to access this resource.");
        } else {
            error = HttpStatusError.toError(HttpStatus.UNAUTHORIZED, request.getPath(),
                "No valid authentication token found in request."
            );
        }
        return HttpResponse.status(HttpStatus.valueOf(error.getCode())).body(error);
    }
}
