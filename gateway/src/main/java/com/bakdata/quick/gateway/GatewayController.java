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

package com.bakdata.quick.gateway;

import com.bakdata.quick.common.api.model.gateway.SchemaData;
import com.bakdata.quick.common.exception.BadArgumentException;
import graphql.GraphQLException;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.errors.SchemaProblem;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.reactivex.Completable;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway's REST API for controlling current schema.
 */
@Slf4j
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GatewayController {
    private final QuickGraphQLContext graphQLContext;

    @Inject
    public GatewayController(final QuickGraphQLContext graphQLContext) {
        this.graphQLContext = graphQLContext;
    }

    /**
     * Updates the GraphQL server with the new schema.
     */
    @Post("/control/schema")
    public Completable buildGraphQL(@Body final SchemaData schema) {
        log.debug("Apply new schema: {}", schema);
        return Completable.fromRunnable(() -> {
            try {
                this.graphQLContext.updateFromSchemaString(schema.getSchema());
            } catch (final SchemaProblem schemaProblem) {
                final String errorMessage = createSchemaErrorMessage(schemaProblem);
                throw new BadArgumentException(errorMessage);
            } catch (final GraphQLException graphQLException) {
                throw new BadArgumentException(graphQLException.getMessage());
            }
        });
    }

    /**
     * Returns schema for a type.
     *
     * <p>
     * The schema does not represent the GraphQL view but the write (as in Kafka) view.
     */
    @Get("/schema/{type}")
    public SchemaData getWriteSchema(final String type) {
        final GraphQLObjectType graphQLType =
            this.graphQLContext.getGraphQLSchema().getObjectType(StringUtils.capitalize(type));
        if (graphQLType == null) {
            throw new BadArgumentException(String.format("Type %s does not exist", type));
        }
        final String schema = TypePrinter.printTypeSchema(graphQLType);
        return new SchemaData(schema);
    }

    private static String createSchemaErrorMessage(final SchemaProblem graphQLException) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Could not parse GraphQL schema: ");
        graphQLException.getErrors().forEach(error -> {
            builder.append("\n");
            builder.append("- ");
            builder.append(error.getMessage());
        });

        return builder.toString();
    }

}
