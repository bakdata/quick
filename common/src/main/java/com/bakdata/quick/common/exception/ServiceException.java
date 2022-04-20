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
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * A service exception is thrown when a request to another quick service fails.
 *
 * <p>
 * This exception expects that the service responds with an {@link ErrorMessage}, which is forwarded.
 */
@Getter
public class ServiceException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public ServiceException(final ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Singleton
    static class ServiceExceptionHandler implements ExceptionHandler<ServiceException, HttpResponse<ErrorMessage>> {
        @Override
        public HttpResponse<ErrorMessage> handle(final HttpRequest request, final ServiceException exception) {
            return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(exception.getErrorMessage().getCode()))
                .body(exception.getErrorMessage());
        }
    }

}
