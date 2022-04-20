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
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;
import org.apache.avro.AvroMissingFieldException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.AvroTypeException;
import org.apache.avro.UnresolvedUnionException;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

/**
 * Exception handler for all exceptions that can be thrown by Avro.
 */
@Singleton
public class AvroExceptionHandler implements ExceptionHandler<AvroRuntimeException, HttpResponse<ErrorMessage>> {
    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final AvroRuntimeException exception) {
        final ErrorMessage errorMessage;
        final String detail;

        if (exception instanceof AvroTypeException
            || exception instanceof AvroMissingFieldException
            || exception instanceof UnresolvedUnionException) {
            detail = String.format("Could not parse input: %s", exception.getMessage());
        } else if (exception instanceof AvroConversionException) {
            detail = String.format("An Error occurred during conversion of the Avro: %s", exception.getMessage());
        } else {
            errorMessage = HttpStatusError
                .toError(HttpStatus.INTERNAL_SERVER_ERROR, request.getPath(), exception.getMessage());
            return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(errorMessage.getCode())).body(errorMessage);
        }
        errorMessage = HttpStatusError.toError(HttpStatus.BAD_REQUEST, request.getPath(), detail);
        return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(errorMessage.getCode())).body(errorMessage);
    }
}
