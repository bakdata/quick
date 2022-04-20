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
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;

/**
 * Exception handler for errors occurring when communicating with the Schema Registry Rest service.
 */
@Singleton
public class SchemaRegistryExceptionHandler
    implements ExceptionHandler<RestClientException, HttpResponse<ErrorMessage>> {

    public static final String UNKNOWN_ERROR = "The schema registry responded with an unknown error";

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final RestClientException exception) {
        // strip error code as it is not relevant for user

        final String message;
        if (exception.getMessage() != null) {
            final int i = exception.getMessage().indexOf("; error code: ");
            message = exception.getMessage().substring(0, i);
        } else {
            message = UNKNOWN_ERROR;
        }
        final ErrorMessage errorMessage = ErrorMessage.builder()
            .type("error/schema")
            .code(exception.getStatus())
            .title("Schema Error")
            .detail(message)
            .uriPath(request.getPath())
            .build();

        return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(exception.getStatus())).body(errorMessage);
    }
}
