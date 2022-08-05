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

import com.bakdata.quick.gateway.custom.type.QuickGraphQLType;
import com.bakdata.quick.gateway.directives.QuickDirectiveWiring;
import graphql.Scalars;
import graphql.language.ScalarTypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaGeneratorPostProcessing;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;

/**
 * Responsible for creating a new GraphQLSchema from a string.
 *
 * <p>
 * The schema generator adds quick specific behavior to GraphQL, like:
 * <ul>
 *     <li>{@link com.bakdata.quick.gateway.directives.topic.TopicDirectiveWiring}</li>
 *     <li>{@link com.bakdata.quick.gateway.directives.rest.RestDirectiveWiring}</li>
 *     <li>{@link com.bakdata.quick.gateway.transformer.MultiSubscriptionTransformer}</li>
 *     <li>{@link com.bakdata.quick.gateway.fetcher.DeferFetcher}</li>
 *     <li>{@link com.bakdata.quick.gateway.custom.QuickPropertyDataFetcher}</li>
 * </ul>
 */
@Singleton
public class GraphQLSchemaGenerator {
    private final List<QuickDirectiveWiring> directiveWirings;
    private final List<QuickGraphQLType<?>> quickGraphQLTypes;
    private final List<SchemaGeneratorPostProcessing> postProcessings;
    private final List<GraphQLScalarType> customScalars;

    /**
     * Injectable constructor.
     *
     * @param directiveWirings  wirings for implementing custom directives
     * @param quickGraphQLTypes additional GraphQL types specific to Quick
     * @param postProcessings   post processor for the GraphQL schema
     */
    @Inject
    public GraphQLSchemaGenerator(final List<QuickDirectiveWiring> directiveWirings,
                                  final List<QuickGraphQLType<?>> quickGraphQLTypes,
                                  final List<SchemaGeneratorPostProcessing> postProcessings) {
        this.directiveWirings = directiveWirings;
        this.quickGraphQLTypes = quickGraphQLTypes;
        this.postProcessings = postProcessings;
        this.customScalars = List.of(ExtendedScalars.GraphQLLong);
    }

    // TODO: remove. Currently exists only for old tests
    public GraphQLSchemaGenerator(final List<QuickDirectiveWiring> directiveWirings) {
        this(directiveWirings, Collections.emptyList(), Collections.emptyList());
    }


    /**
     * Creates a new GraphQL schema from a string.
     *
     * <p>
     * The schema generator adds quick specific behavior to GraphQL.
     */
    public GraphQLSchema create(final String schema) {
        final RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        final TypeDefinitionRegistry baseRegistry = new TypeDefinitionRegistry();

        for (final QuickDirectiveWiring directive : this.directiveWirings) {
            builder.directive(directive.getName(), directive);
            baseRegistry.add(directive.getDefinition());
        }

        for (final QuickGraphQLType<?> quickGraphQLType : this.quickGraphQLTypes) {
            baseRegistry.add(quickGraphQLType.getDefinition());
        }

        for (final GraphQLScalarType customScalar : this.customScalars) {
            baseRegistry.add(new ScalarTypeDefinition(customScalar.getName()));
            builder.scalar(customScalar);
        }
        this.postProcessings.forEach(builder::transformer);
        //builder.wiringFactory(QuickWiringFactory.create());

        final TypeDefinitionRegistry userRegistry = new SchemaParser().parse(schema);
        baseRegistry.merge(userRegistry);
        return new SchemaGenerator().makeExecutableSchema(baseRegistry, builder.build());
    }
}
