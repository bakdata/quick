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

import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple wrapper around GraphQL implementation to make swapping schema easier.
 */
@Singleton
public class QuickGraphQLContext {
    private static final GraphQLSchema EMPTY_SCHEMA = GraphQLSchema.newSchema()
        .query(GraphQLObjectType.newObject().name("Empty").build())
        .build();

    private final GraphQLSchemaGenerator schemaGenerator;


    /**
     * Functions that are called whenever the GraphQL schema is updated.
     */
    private final List<Consumer<QuickGraphQLContext>> updateCallbacks;

    private GraphQL graphQL;
    private GraphQLSchema graphQLSchema;

    /**
     * Standard constructor.
     *
     * @param schemaGenerator generator for creating a GraphQLSchema from a string
     */
    public QuickGraphQLContext(final GraphQLSchemaGenerator schemaGenerator) {
        this.updateCallbacks = new ArrayList<>();
        this.schemaGenerator = schemaGenerator;
        this.graphQLSchema = EMPTY_SCHEMA;
        this.graphQL = GraphQL.newGraphQL(EMPTY_SCHEMA).build();
    }

    /**
     * Updates the GraphQLSchema.
     */
    public void update(final GraphQLSchema schema) {
        this.graphQLSchema = schema;
        this.graphQL = GraphQL.newGraphQL(schema).build();
        this.updateCallbacks.forEach(consumer -> consumer.accept(this));
    }

    /**
     * Creates a new GraphQLSchema from a schema string and updates it.
     */
    public void updateFromSchemaString(final String schemaString) {
        this.update(this.schemaGenerator.create(schemaString));
    }

    public GraphQL getGraphQL() {
        return this.graphQL;
    }

    public GraphQLSchema getGraphQLSchema() {
        return this.graphQLSchema;
    }

    public void addGraphQLUpdateCallback(final Consumer<QuickGraphQLContext> callback) {
        this.updateCallbacks.add(callback);
    }

    /**
     * This simply creates an empty GraphQL Bean to satisfy the requirement imposed by {@link
     * io.micronaut.configuration.graphql.ws.RequiresGraphQLWs}. If no GraphQL beans exists, micronaut's graphql-ws
     * package is not loaded.
     */
    @Factory
    static class DummyFactory {
        @Singleton
        GraphQL dummyGraphQLObject() {
            return GraphQL.newGraphQL(EMPTY_SCHEMA).build();
        }
    }
}

