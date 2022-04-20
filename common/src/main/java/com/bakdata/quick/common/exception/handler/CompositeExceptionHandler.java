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
import com.google.common.base.Throwables;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.reactivex.exceptions.CompositeException;
import java.util.Optional;
import javax.inject.Singleton;

/**
 * Exception handler handling {@link CompositeException}.
 *
 * <p>
 * Composite exceptions can collect multiple exceptions. The handler creates a messages containing all errors.
 */
@Singleton
public class CompositeExceptionHandler implements ExceptionHandler<CompositeException, HttpResponse<ErrorMessage>> {

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final CompositeException exception) {
        final StringBuilder messageBuilder = new StringBuilder();
        exception.getExceptions().forEach(nestedException -> reduceErrorMessages(messageBuilder, nestedException));

        final ErrorMessage errorMessage = ErrorMessage.builder()
            .type("errors/composite")
            .title(exception.getExceptions().size() + " errors occurred")
            .uriPath(request.getPath())
            .detail(messageBuilder.toString())
            .code(500)
            .build();

        return HttpResponse.serverError(errorMessage);
    }

    @Nullable
    private static String parseClientException(final HttpClientResponseException rootCause) {
        final Optional<ErrorMessage> body = rootCause.getResponse().getBody(Argument.of(ErrorMessage.class));
        if (body.isPresent()) {
            return String.format("%s: %s", body.get().getTitle(), body.get().getDetail());
        } else {
            return rootCause.getMessage();
        }
    }

    private static void reduceErrorMessages(final StringBuilder messageBuilder, final Throwable nestedException) {
        messageBuilder.append("\n\t");
        final Throwable rootCause = Throwables.getRootCause(nestedException);
        if (rootCause instanceof HttpClientResponseException) {
            messageBuilder.append(parseClientException((HttpClientResponseException) rootCause));
        } else {
            messageBuilder.append(rootCause.getMessage());
        }
    }
}
