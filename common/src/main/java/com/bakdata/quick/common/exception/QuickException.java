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

package com.bakdata.quick.common.exception;

import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.HttpStatusError;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import java.util.Objects;
import javax.inject.Singleton;

/**
 * Base class for exceptions in quick. It includes a handler for converting subclasses to error messages.
 */
public abstract class QuickException extends RuntimeException {

    public static final String MESSAGE = "No error message was produced.";

    protected QuickException(@Nullable final String message) {
        super(Objects.requireNonNull(message, MESSAGE));
    }

    public QuickException(final String message, final Throwable cause) {
        super(message, cause);
    }

    protected abstract HttpStatus getStatus();

    /**
     * Fallback exception handler for subclasses.
     */
    @Singleton
    public static class QuickExceptionHandler implements ExceptionHandler<QuickException, HttpResponse<ErrorMessage>> {

        @Override
        public HttpResponse<ErrorMessage> handle(final HttpRequest request, final QuickException exception) {
            final ErrorMessage errorMessage =
                HttpStatusError.toError(exception.getStatus(), request.getPath(), exception.getMessage());
            return HttpResponse.<ErrorMessage>status(exception.getStatus())
                .body(errorMessage);
        }
    }
}
