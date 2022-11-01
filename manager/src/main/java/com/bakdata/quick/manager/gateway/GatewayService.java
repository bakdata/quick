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
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;

/**
 * Service controlling gateways.
 */
public interface GatewayService {

    /**
     * Lists all gateways.
     *
     * @return {@link GatewayDescription} object
     */
    Single<List<GatewayDescription>> getGatewayList(final String requestId);

    /**
     * Displays information about a single gateway.
     *
     * @param name of the gateway requested
     * @return {@link GatewayDescription} object
     */
    Single<GatewayDescription> getGateway(final String name, final String requestId);

    /**
     * Deploys a new gateway.
     *
     * @param gatewayCreationData data for creating this gateway
     */
    Completable createGateway(final GatewayCreationData gatewayCreationData, final String requestId);

    /**
     * Deletes a gateway.
     *
     * @param name the name of the gateway to delete
     */
    Completable deleteGateway(final String name, final String requestId);

    /**
     * Updates the schema of a gateway.
     *
     * @param name          name of the gateway
     * @param graphQLSchema new schema to update
     */
    Completable updateSchema(final String name, final String graphQLSchema, final String requestId);

    /**
     * Returns the write schema for a given gateway in GraphQL or Avro format.
     *
     * @param name   the name of the gateway
     * @param type   the type used in the write schema
     * @param format enum defining in which format the write schema should be returned
     * @return SchemaData object containing the write schema
     */
    Single<SchemaData> getGatewayWriteSchema(final String name, final String type, final SchemaFormat format,
                                             final String requestId);

    /**
     * The enum denotes the schema format returned by the gateway.
     */
    enum SchemaFormat {
        GRAPHQL("GraphQL"),
        AVRO("Avro");

        private final String name;

        SchemaFormat(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
