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

package com.bakdata.quick.manager.gateway;

import static io.micronaut.http.HttpRequest.DELETE;
import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakdata.quick.common.api.client.GatewayClient;
import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.manager.gateway.GatewayService.SchemaFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


@MicronautTest
class GatewayControllerTest {

    public static final String BASE_PATH = "/gateway/{name}";
    private static final String GATEWAY_NAME = "test-gateway";
    private static final String TAG = "test-version";
    private static final int DEFAULT_REPLICA = 1;
    private static String baseUri = null;

    @Client("/")
    @Inject
    RxHttpClient client;

    @Inject
    GatewayService gatewayService;

    @Inject
    GatewayClient gatewayClient;

    @BeforeAll
    static void init() {
        baseUri = UriBuilder.of(BASE_PATH)
            .expand(Collections.singletonMap("name", GATEWAY_NAME))
            .toString();
        assertEquals(String.format("/gateway/%s", GATEWAY_NAME), baseUri);
    }

    @Test
    void shouldGetGatewayList() {
        when(this.gatewayService.getGatewayList())
            .thenReturn(Single.just(List.of(new GatewayDescription(GATEWAY_NAME, 1, TAG))));

        this.client.toBlocking().exchange(GET("/gateways"));

        verify(this.gatewayService).getGatewayList();
    }

    @Test
    void shouldGetGateway() {
        when(this.gatewayService.getGateway(anyString()))
            .thenReturn(Single.just(new GatewayDescription(GATEWAY_NAME, DEFAULT_REPLICA, TAG)));

        this.client.toBlocking().exchange(GET(baseUri));

        verify(this.gatewayService).getGateway(GATEWAY_NAME);
    }

    @Test
    void shouldCreateGateway() throws JsonProcessingException {
        when(this.gatewayService.createGateway(isNotNull())).thenReturn(Completable.complete());

        final GatewayCreationData creationData = new GatewayCreationData(GATEWAY_NAME, 1, TAG, null);
        this.client.toBlocking().exchange(POST("/gateway", new ObjectMapper().writeValueAsString(creationData)));

        verify(this.gatewayService).createGateway(creationData);
    }

    @Test
    void shouldDeleteGateway() {
        when(this.gatewayService.deleteGateway(anyString())).thenReturn(Completable.complete());

        this.client.toBlocking().exchange(DELETE(baseUri));

        verify(this.gatewayService).deleteGateway(GATEWAY_NAME);
    }

    @Test
    void shouldCreateDefinition() {
        when(this.gatewayService.updateSchema(anyString(), anyString())).thenReturn(Completable.complete());
        final String uri = UriBuilder.of(BASE_PATH + "/schema")
            .expand(Collections.singletonMap("name", GATEWAY_NAME))
            .toString();
        assertEquals("/gateway/test-gateway/schema", uri);

        this.client.toBlocking().exchange(POST(uri, new SchemaData("type Query { test: String }")));

        verify(this.gatewayService).updateSchema(GATEWAY_NAME, "type Query { test: String }");
    }

    @Test
    void shouldGetGatewayGraphQLSchema() {
        final String schema = "type Test { test: Int }";

        final SchemaData schemaData = new SchemaData(schema);
        final GatewayDescription gatewayDescription = new GatewayDescription(GATEWAY_NAME, 1, null);

        when(this.gatewayService.getGateway(GATEWAY_NAME))
            .thenReturn(Single.just(gatewayDescription));

        when(this.gatewayService.getGatewayWriteSchema(GATEWAY_NAME, "Test", SchemaFormat.GRAPHQL))
            .thenReturn(Single.just(schemaData));

        final String uri = UriBuilder.of(BASE_PATH + "/schema/{type}/graphql")
            .expand(Map.of("name", GATEWAY_NAME, "type", "Test"))
            .toString();
        assertEquals("/gateway/test-gateway/schema/Test/graphql", uri);

        final SchemaData graphqlSchema = this.client.toBlocking().retrieve(GET(uri), SchemaData.class);

        assertThat(graphqlSchema).isEqualTo(new SchemaData(schema));
    }

    @Test
    void shouldGetGatewayAvroSchema() throws IOException {
        final Path workingDirectory = Path.of("src", "test", "resources", "schema", "avro");
        final String avroSchema =
            new Schema.Parser().parse(Files.readString(workingDirectory.resolve("test.avsc"))).toString();

        final SchemaData schemaData = new SchemaData(avroSchema);
        final GatewayDescription gatewayDescription = new GatewayDescription(GATEWAY_NAME, 1, null);

        when(this.gatewayService.getGateway(GATEWAY_NAME))
            .thenReturn(Single.just(gatewayDescription));

        when(this.gatewayService.getGatewayWriteSchema(GATEWAY_NAME, "Test", SchemaFormat.AVRO))
            .thenReturn(Single.just(schemaData));

        final String uri = UriBuilder.of(BASE_PATH + "/schema/{type}/avro")
            .expand(Map.of("name", GATEWAY_NAME, "type", "Test"))
            .toString();
        assertEquals("/gateway/test-gateway/schema/Test/avro", uri);

        final SchemaData retrievedAvroSchema = this.client.toBlocking().retrieve(GET(uri), SchemaData.class);

        assertThat(retrievedAvroSchema).isEqualTo(schemaData);
    }

    @MockBean(GatewayService.class)
    GatewayService gatewayService() {
        return mock(GatewayService.class);
    }

    @MockBean(GatewayClient.class)
    GatewayClient gatewayClient() {
        return mock(GatewayClient.class);
    }

}
