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
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.hateoas.JsonError;
import io.reactivex.Flowable;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

/**
 * Convert {@link JsonError} to {@link ErrorMessage}.
 *
 * <p>
 * {@link JsonError} is Micronaut's internal error type and we must transform it into {@link
 * com.bakdata.quick.common.api.model.ErrorMessage}. If responses with status codes 4xx and 5xx are encountered, the
 * filter checks for a body with {@link JsonError}. Then it transforms it into our custom error type.
 */

@Filter(Filter.MATCH_ALL_PATTERN)
@Slf4j
public class JsonErrorExceptionHandler implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(final HttpRequest<?> request, final ServerFilterChain chain) {
        request.mutate();
        return Flowable.fromPublisher(chain.proceed(request))
            .map(response -> {
                // check all 4xx and 5xx (client and server errors)
                if (response.getStatus().getCode() >= 400) {
                    final Optional<JsonError> body = response.getBody(JsonError.class);
                    if (body.isPresent()) {
                        log.trace("Convert JSON error to custom error message");
                        final JsonError jsonError = body.get();
                        final ErrorMessage errorMessage = HttpStatusError
                            .toError(response.getStatus(), request.getPath(), jsonError.getMessage());
                        return HttpResponse.status(response.getStatus()).body(errorMessage);
                    }
                }
                return response;
            });
    }
}
