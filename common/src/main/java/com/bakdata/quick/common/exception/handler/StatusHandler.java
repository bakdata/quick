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
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import javax.inject.Singleton;

/**
 * Utility for converting any http response to a {@link ErrorMessage}.
 */
@Singleton
public class StatusHandler {
    public MutableHttpResponse<ErrorMessage> handle(final HttpRequest<?> request, final HttpStatus status,
        @Nullable final String message) {
        final ErrorMessage error = HttpStatusError.toError(status, request.getPath(), message);
        return HttpResponse.<ErrorMessage>status(status).body(error);
    }
}
