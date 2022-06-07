package com.bakdata.quick.manager.graphql;

import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.common.type.QuickTopicType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.confluent.kafka.schemaregistry.ParsedSchema;

public interface GraphQLConverter {

    ParsedSchema convert(String graphQLSchema);

    default GraphQLObjectType getRootTypeFromSchema(String schema) {
        // extending the schema with an empty query type is necessary because parsing fails otherwise
        final SchemaParser schemaParser = new SchemaParser();

        // extending the schema with an empty query type is necessary because parsing fails otherwise
        final String extendedSchema = schema + "type Query{}";
        final TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(extendedSchema);
        final String rootTypeName = GraphQLUtils.getRootType(QuickTopicType.SCHEMA, typeDefinitionRegistry);

        // existence required for building a GraphQLSchema, no wiring needed otherwise
        final RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return graphQLSchema.getObjectType(rootTypeName);
    }
}
