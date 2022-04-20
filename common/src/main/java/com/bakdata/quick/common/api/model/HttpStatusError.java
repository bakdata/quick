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

package com.bakdata.quick.common.api.model;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;
import java.util.Objects;
import lombok.Getter;

/**
 * Types of http errors.
 */
@Getter
public enum HttpStatusError {
    CLIENT_ERROR("errors/clientError"),
    SERVER_ERROR("errors/serverError");

    private final String type;

    HttpStatusError(final String type) {
        this.type = type;
    }

    /**
     * Converts this to an {@link ErrorMessage}.
     */
    public static ErrorMessage toError(final HttpStatus status, final String uriPath, @Nullable final String detail) {
        final String errorMessage = Objects.requireNonNullElse(detail, "Unknown error.");
        return ErrorMessage.builder()
            .type(forStatus(status).type)
            .title(status.getReason())
            .code(status.getCode())
            .detail(errorMessage)
            .uriPath(uriPath)
            .build();
    }

    private static HttpStatusError forStatus(final HttpStatus status) {
        if (status.getCode() >= 400 && status.getCode() < 500) {
            return CLIENT_ERROR;
        } else {
            return SERVER_ERROR;
        }
    }
}
