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

package com.bakdata.quick.gateway.fetcher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.directives.rest.RestParameter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphqlErrorException;
import graphql.Scalars;
import graphql.language.FieldDefinition;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLFieldDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class RestDataFetcherTest {
    private final MockWebServer mockWebServer = new MockWebServer();
    private final String url = this.mockWebServer.url("/").toString();
    private final ObjectMapper mapper = new ObjectMapper();

    // required for error messages
    private final SourceLocation sourceLocation = new SourceLocation(5, 5);
    private final GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
        .name("test")
        .type(Scalars.GraphQLString)
        .definition(FieldDefinition.newFieldDefinition()
            .sourceLocation(this.sourceLocation)
            .build())
        .build();

    @Test
    void shouldForwardPathParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", "myId"))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        final RecordedRequest recordedRequest = this.mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/myId");
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldForwardPathParameterInOrder() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final List<RestParameter> pathArguments = List.of(
            new RestParameter("type", true),
            new RestParameter("id", true)
        );
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, pathArguments, List.of(), null, true,
                RestDirectiveMethod.GET
            );

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", "myId", "type", "test"))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/test/myId");
    }

    @Test
    void shouldForwardIntPathParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/5");
    }

    @Test
    void shouldForwardFloatPathParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5.2))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/5.2");
    }

    @Test
    void shouldForwardComplexPathParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", Map.of("nested", "value")))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/%7Bnested=value%7D");
    }

    @Test
    void shouldForwardQueryParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(), List.of(new RestParameter("id", true)), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", "myId"))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/?id=myId");
    }

    @Test
    void shouldForwardIntQueryParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(), List.of(new RestParameter("id", true)), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/?id=5");
    }

    @Test
    void shouldForwardFloatQueryParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(), List.of(new RestParameter("id", true)), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5.2))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/?id=5.2");
    }

    @Test
    void shouldForwardComplexQueryParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(), List.of(new RestParameter("id", true)), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", Map.of("nested", "value")))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/?id=%7Bnested%3Dvalue%7D");
    }

    @Test
    void shouldForwardMultipleParameters() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final List<RestParameter> pathArguments = List.of(
            new RestParameter("type", true),
            new RestParameter("id", true)
        );
        final List<RestParameter> queryArguments = List.of(
            new RestParameter("limit", true),
            new RestParameter("offset", true)
        );

        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, pathArguments, queryArguments, null, true,
                RestDirectiveMethod.GET);

        final Map<String, Object> id = Map.of("id", "myId", "type", "test", "limit", 5, "offset", 10);
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(id)
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/test/myId?limit=5&offset=10");
    }

    @Test
    void shouldAllowNullInQueryParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(), List.of(new RestParameter("id", true)), null,
                true,
                RestDirectiveMethod.GET);

        final HashMap<String, Object> parameter = new HashMap<>();
        parameter.put("id", null);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(parameter)
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/");
    }

    @Test
    void shouldAllowNullableResult() throws Exception {
        this.mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isNull();
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/5");
    }

    @Test
    void shouldNotAllowNonNullableResultFor404() throws Exception {
        this.mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                false,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5))
            .fieldDefinition(this.fieldDefinition)
            .build();

        // type doesn't really matter since
        assertThatExceptionOfType(GraphqlErrorException.class).isThrownBy(() -> fetcher.get(env))
            .withMessage("REST service responded with code 404")
            .extracting(GraphqlErrorException::getLocations, InstanceOfAssertFactories.list(SourceLocation.class))
            .hasSize(1)
            .first()
            .isEqualTo(this.sourceLocation);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/5");
    }

    @Test
    void shouldNotAllowNonNullableResultForEmptyBody() throws Exception {
        this.mockWebServer.enqueue(new MockResponse());
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                false,
                RestDirectiveMethod.GET);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", 5))
            .fieldDefinition(this.fieldDefinition)
            .build();

        // type doesn't really matter since
        assertThatExceptionOfType(GraphqlErrorException.class).isThrownBy(() -> fetcher.get(env))
            .withMessage("The REST service didn't respond with a body")
            .extracting(GraphqlErrorException::getLocations, InstanceOfAssertFactories.list(SourceLocation.class))
            .hasSize(1)
            .first()
            .isEqualTo(this.sourceLocation);
        assertThat(this.mockWebServer.takeRequest().getPath()).isEqualTo("/5");
    }

    @Test
    void shouldThrowExceptionForNullPathParameter() {
        this.mockWebServer.enqueue(new MockResponse());
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                false,
                RestDirectiveMethod.GET);

        final Map<String, Object> parameter = new HashMap<>();
        parameter.put("id", null);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(parameter)
            .fieldDefinition(this.fieldDefinition)
            .build();

        // type doesn't really matter since
        assertThatExceptionOfType(GraphqlErrorException.class).isThrownBy(() -> fetcher.get(env))
            .withMessageStartingWith("id is used as a path parameter and is thus required")
            .extracting(GraphqlErrorException::getLocations, InstanceOfAssertFactories.list(SourceLocation.class))
            .hasSize(1)
            .first()
            .isEqualTo(this.sourceLocation);
    }

    @Test
    void shouldUsePost() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(), null,
                true,
                RestDirectiveMethod.POST);

        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", "myId"))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        final RecordedRequest recordedRequest = this.mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/myId");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }


    @Test
    void shouldUseBodyParameter() throws Exception {
        final Map<String, String> result = Map.of("value", "success");
        final String productJson = this.mapper.writeValueAsString(result);
        this.mockWebServer.enqueue(new MockResponse().setBody(productJson));
        final RestDataFetcher<?> fetcher =
            new RestDataFetcher<>(new HttpClient(), this.url, List.of(new RestParameter("id", true)), List.of(),
                new RestParameter("inputs", false), true, RestDirectiveMethod.POST);

        final Map<String, List<Integer>> body = Map.of("data", List.of(1, 2, 3));
        final DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .arguments(Map.of("id", "myId", "inputs", body))
            .build();

        final Object obj = fetcher.get(env);
        assertThat(obj).isEqualTo(result);
        final RecordedRequest recordedRequest = this.mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/myId");
        assertThat(recordedRequest.getBody()).isNotNull();
        Map<String, Object> receivedBody =
            this.mapper.readValue(recordedRequest.getBody().readByteArray(), new TypeReference<Map<String, Object>>() {
            });
        assertThat(receivedBody).isEqualTo(body);

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }
}
