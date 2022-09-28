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

package com.bakdata.quick.common.api.client.gateway;

import com.bakdata.quick.common.api.model.gateway.SchemaData;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Client for the gateway's REST API.
 *
 * <p>
 * Note: This client contains only the REST API and therefore provides no access to the GraphQL interface.
 */
public interface GatewayClient {
    /**
     * Updates the GraphQL schema.
     *
     * @param gateway       name of the gateway
     * @param graphQLSchema GraphQL schema
     * @return completed if schema is updated
     */
    Completable updateSchema(final String gateway, final SchemaData graphQLSchema);

    /**
     * Fetches a write-type of a gateway.
     *
     * @param gateway name of the gateway
     * @param type    name of the type
     * @return write schema if gateway and type exist
     */
    Single<SchemaData> getWriteSchema(final String gateway, final String type);
}
