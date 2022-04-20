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

import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.exception.HttpClientException;
import com.bakdata.quick.common.security.SecurityConfig;
import io.micronaut.http.HttpStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client for multiple gateways' rest APIs.
 */
@Singleton
public class DefaultGatewayClient implements GatewayClient {
    private static final MediaType JSON
        = MediaType.get("application/json; charset=utf-8");
    private static final String PREFIX = "quick-gateway";

    private final HttpClient client;
    private final SecurityConfig config;

    @Inject
    public DefaultGatewayClient(final HttpClient client, final SecurityConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Completable updateSchema(final String gateway, final SchemaData graphQLSchema) {
        return Single.fromCallable(() -> this.client.objectMapper().writeValueAsString(graphQLSchema))
            .map(json -> {
                final Request request = new Request.Builder()
                    .post(RequestBody.create(json, JSON))
                    .url(String.format("http://%s-%s/control/schema", PREFIX, gateway))
                    .header("X-API-Key", this.config.getApiKey())
                    .build();

                return this.client.newCall(request).execute();
            })
            .flatMapCompletable(response -> {
                if (response.isSuccessful()) {
                    return Completable.complete();
                }
                return Completable.error(handleError(response));
            });
    }


    @Override
    public Single<SchemaData> getWriteSchema(final String gateway, final String type) {
        return Single.fromCallable(() -> String.format("http://%s-%s/schema/%s", PREFIX, gateway, type))
            .map(url -> {
                final Request request = new Request.Builder()
                    .get()
                    .url(url)
                    .header("X-API-Key", this.config.getApiKey())
                    .build();

                return this.client.newCall(request).execute();
            })
            .flatMap(response -> {
                if (response.isSuccessful()) {
                    final ResponseBody body = Objects.requireNonNull(response.body());
                    return Single.fromCallable(
                        () -> this.client.objectMapper().readValue(body.byteStream(), SchemaData.class));
                }
                return Single.error(handleError(response));
            });
    }

    private static Throwable handleError(final Response response) {
        final HttpStatus httpStatus = HttpStatus.valueOf(response.code());
        try {
            final String message = response.body().string();
            return new HttpClientException(message, httpStatus);
        } catch (final RuntimeException | IOException exception) {
            return new HttpClientException(httpStatus);
        }
    }

}
