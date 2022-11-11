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

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@MicronautTest
public class RequestHeaderFilterTest {

    private static final String TEST_HEADER_VALUE_1 = "da75ca2a-84b4-46c3-9247-d4f2dc44159f";
    private static final String TEST_HEADER_VALUE_2 = "99789c15-9ab7-4c94-b049-eb78abed688b";
    private static EmbeddedServer server;
    private static HttpClient client;


    @BeforeAll
    public static void setUp() {
        server = ApplicationContext.run(EmbeddedServer.class);
        client = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
    }

    @AfterAll
    public static void tearDown() {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldSetHeaderValueIfNotPresent() {
        final HttpResponse<?> response = client.toBlocking().exchange("/test/header");
        final String headerValue = response.getHeaders().get(HeaderConstants.REQUEST_HEADER);
        assertThat(headerValue).isNotNull();
        assertThat(headerValue).isEqualTo(TEST_HEADER_VALUE_1);
    }

    @Test
    void shouldGetHeaderValueIfAlreadySet() {
        final MutableHttpRequest<?> request = HttpRequest.GET("/test/header");
        request.header(HeaderConstants.REQUEST_HEADER, TEST_HEADER_VALUE_2);
        final HttpResponse<?> response = client.toBlocking().exchange(request);
        final String headerValue = response.getHeaders().get(HeaderConstants.REQUEST_HEADER);
        assertThat(headerValue).isEqualTo(TEST_HEADER_VALUE_2);
    }


    @Controller("/test")
    @Secured(SecurityRule.IS_ANONYMOUS)
    static class TestController {

        @Get("/header")
        public HttpResponse<String> issue(final HttpHeaders headers) {
            return HttpResponse.ok("Response with the X-Request-ID header").header(
                HeaderConstants.REQUEST_HEADER, headers.get(HeaderConstants.REQUEST_HEADER)
            );
        }
    }


    @Replaces(Supplier.class)
    @Singleton
    public static class SupplierMock implements Supplier<UUID> {
        @Override
        public UUID get() {
            return UUID.fromString(TEST_HEADER_VALUE_1);
        }
    }
}
