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
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.connect.errors.AlreadyExistsException;
import org.apache.kafka.connect.errors.NotFoundException;

/**
 * Exception handler for all exceptions that can be thrown by Kafka.
 */
@Singleton
public class KafkaExceptionHandler implements ExceptionHandler<KafkaException, HttpResponse<ErrorMessage>> {
    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final KafkaException exception) {
        final ErrorMessage errorMessage;
        if (exception instanceof NotFoundException) {
            errorMessage = HttpStatusError.toError(HttpStatus.NOT_FOUND, request.getPath(), exception.getMessage());
        } else if (exception instanceof AlreadyExistsException) {
            errorMessage = HttpStatusError.toError(HttpStatus.CONFLICT, request.getPath(), exception.getMessage());
        } else if (exception instanceof TopicExistsException
            || exception instanceof UnknownTopicOrPartitionException
            || exception instanceof InvalidTopicException) {
            errorMessage = HttpStatusError.toError(HttpStatus.BAD_REQUEST, request.getPath(), exception.getMessage());
        } else {
            errorMessage = HttpStatusError
                .toError(HttpStatus.INTERNAL_SERVER_ERROR, request.getPath(), exception.getMessage());
        }
        return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(errorMessage.getCode())).body(errorMessage);
    }
}
