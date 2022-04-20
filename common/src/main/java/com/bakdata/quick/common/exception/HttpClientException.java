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

import io.micronaut.http.HttpStatus;

/**
 * Exception thrown when a client responded with a 400+ status code unexpectedly.
 */
public class HttpClientException extends QuickException {
    private final HttpStatus status;

    public HttpClientException(final HttpStatus status) {
        super(status.getReason());
        this.status = status;
    }

    public HttpClientException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

    @Override
    protected HttpStatus getStatus() {
        return this.status;
    }
}
