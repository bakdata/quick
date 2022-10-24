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

package com.bakdata.quick.manager.graphql;

import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.common.type.QuickTopicType;
import graphql.language.ScalarTypeDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import java.util.List;

/**
 * Converter for transforming a GraphQL schema to a {@link ParsedSchema} object.
 */
public interface GraphQLConverter {

    List<GraphQLScalarType> SCALARS = List.of(
        ExtendedScalars.GraphQLLong,
        ExtendedScalars.GraphQLShort,
        ExtendedScalars.GraphQLChar,
        ExtendedScalars.DateTime
    );

    ParsedSchema convert(String graphQLSchema);

    /**
     * Gets the root (type) object of the GraphQL schema. This object is later parsed by the implemented classes to do
     * the conversion.
     *
     * @param schema The string containing the GraphQL schema.
     * @return a {@link GraphQLObjectType} object, which contains information of the root object in the schema.
     */
    default GraphQLObjectType getRootTypeFromSchema(final String schema) {
        final SchemaParser schemaParser = new SchemaParser();

        // extending the schema with an empty query type is necessary because parsing fails otherwise
        final String extendedSchema = schema + "type Query { placeholder: Boolean }";
        final TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(extendedSchema);
        final String rootTypeName = GraphQLUtils.getRootType(QuickTopicType.SCHEMA, typeDefinitionRegistry);

        final RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();

        for (final GraphQLScalarType scalar : SCALARS) {
            typeDefinitionRegistry.add(new ScalarTypeDefinition(scalar.getName()));
            runtimeWiring.scalar(scalar);
        }

        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        final GraphQLSchema graphQLSchema =
            schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring.build());

        return graphQLSchema.getObjectType(rootTypeName);
    }
}
