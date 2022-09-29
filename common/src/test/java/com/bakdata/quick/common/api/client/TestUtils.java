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

package com.bakdata.quick.common.api.client;

import io.micronaut.http.HttpStatus;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class TestUtils {
    private TestUtils() {
    }

    public static Response mockResponse() {
        final Request mockRequest = new Request.Builder()
            .url("https://some-url.com")
            .build();
        return new Response.Builder()
            .request(mockRequest)
            .protocol(Protocol.HTTP_2)
            .code(HttpStatus.OK.getCode())
            .message("")
            .body(ResponseBody.create(
                "{}",
                MediaType.get("application/json; charset=utf-8")
            ))
            .build();
    }

    public static Response mockResponse(final String body) {
        final Request mockRequest = new Request.Builder()
            .url("https://some-url.com")
            .build();
        return new Response.Builder()
            .request(mockRequest)
            .protocol(Protocol.HTTP_2)
            .code(HttpStatus.OK.getCode())
            .message("")
            .body(ResponseBody.create(
                body,
                MediaType.get("application/json; charset=utf-8")
            ))
            .build();
    }
}
