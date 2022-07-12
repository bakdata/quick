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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.micronaut.context.annotation.Factory;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Quick's default HTTP client.
 */
@Singleton
public class HttpClient {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    /**
     * Default constructor.
     *
     * @param objectMapper json mapper
     * @param httpClient   underlying client
     */
    @Inject
    public HttpClient(final ObjectMapper objectMapper, final OkHttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public HttpClient() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
    }

    public Call newCall(final Request request) {
        return this.httpClient.newCall(request);
    }

    public TypeFactory typeFactory() {
        return this.objectMapper.getTypeFactory();
    }

    public ObjectMapper objectMapper() {
        return this.objectMapper;
    }

    public OkHttpClient okHttpClient() {
        return this.httpClient;
    }

    /**
     * Default factory for the OkHttpClient.
     *
     * <p>
     * This can be overwritten to customize the client.
     */
    @Factory
    public static class OkHttpClientFactory {
        @Singleton
        public OkHttpClient client() {
            return new OkHttpClient();
        }
    }

}
