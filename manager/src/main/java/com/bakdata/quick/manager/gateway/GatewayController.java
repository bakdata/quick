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

import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.api.model.manager.GatewayDescription;
import com.bakdata.quick.common.api.model.manager.creation.GatewayCreationData;
import com.bakdata.quick.manager.gateway.GatewayService.SchemaFormat;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;

/**
 * Manager REST API for controlling gateway.
 */
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GatewayController {
    private final GatewayService gatewayService;

    @Inject
    public GatewayController(final GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * Lists all gateways.
     */
    @Get("/gateways")
    public Single<List<GatewayDescription>> getGatewayList() {
        return this.gatewayService.getGatewayList();
    }

    /**
     * Displays information about a single gateway.
     */
    @Get("/gateway/{name}")
    public Single<GatewayDescription> getGateway(final String name) {
        return this.gatewayService.getGateway(name);
    }

    /**
     * Deploys a new gateway.
     *
     * @param gatewayCreationData user data configuring the gateway's deployment
     */
    @Post("/gateway")
    public Completable createGateway(@Body final GatewayCreationData gatewayCreationData) {
        return this.gatewayService.createGateway(gatewayCreationData);
    }

    /**
     * Deletes a gateway.
     */
    @Delete("/gateway/{name}")
    public Completable deleteGateway(final String name) {
        return this.gatewayService.deleteGateway(name);
    }

    /**
     * Updates the schema of a gateway.
     */
    @Post(value = "/gateway/{name}/schema")
    public Completable updateSchema(final String name, @Body final SchemaData graphQLSchema) {
        return this.gatewayService.updateSchema(name, graphQLSchema.getSchema());
    }

    /**
     * Get write schema of a gateway in GraphQL format.
     */
    @Get(value = "/gateway/{name}/schema/{type}/graphql")
    public Single<SchemaData> getGraphQLWriteSchema(final String name, final String type) {
        return this.gatewayService.getGatewayWriteSchema(name, type, SchemaFormat.GRAPHQL);
    }

    /**
     * Get write schema of a gateway in Avro format.
     */
    @Get(value = "/gateway/{name}/schema/{type}/avro")
    public Single<SchemaData> getAvroWriteSchema(final String name, final String type) {
        return this.gatewayService.getGatewayWriteSchema(name, type, SchemaFormat.AVRO);
    }
}
