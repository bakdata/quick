package com.bakdata.quick.common.exception.handler;

import com.bakdata.quick.common.api.model.ErrorMessage;
import com.bakdata.quick.common.api.model.HttpStatusError;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import javax.inject.Singleton;

@Singleton
public class ProtobufExceptionHandler
    implements ExceptionHandler<DescriptorValidationException, HttpResponse<ErrorMessage>> {

    @Override
    public HttpResponse<ErrorMessage> handle(final HttpRequest request, final DescriptorValidationException exception) {
        final ErrorMessage errorMessage;

        errorMessage = HttpStatusError.toError(HttpStatus.BAD_REQUEST,
            request.getPath(),
            String.format("An Error occurred during conversion of the Proto: %s", exception.getMessage()));
        return HttpResponse.<ErrorMessage>status(HttpStatus.valueOf(errorMessage.getCode())).body(errorMessage);
    }
}
