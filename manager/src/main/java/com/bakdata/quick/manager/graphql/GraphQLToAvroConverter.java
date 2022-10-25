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

import static graphql.schema.GraphQLTypeUtil.isEnum;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.schema.GraphQLTypeUtil.isScalar;
import static graphql.schema.GraphQLTypeUtil.unwrapOne;

import com.bakdata.quick.common.condition.AvroSchemaFormatCondition;
import com.bakdata.quick.common.config.AvroConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.google.common.annotations.VisibleForTesting;
import graphql.Scalars;
import graphql.language.EnumValueDefinition;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.avro.Schema;

/**
 * Converts a GraphQL Schema to an Avro Schema.
 */
@Singleton
@Requires(condition = AvroSchemaFormatCondition.class)
public final class GraphQLToAvroConverter implements GraphQLConverter {
    @Getter
    private final String avroNamespace;
    private static final Map<String, Schema.Type> SCALAR_MAPPING = scalarTypeMap();

    @Inject
    public GraphQLToAvroConverter(final AvroConfig avroConfig) {
        this(avroConfig.getNamespace());
    }

    @VisibleForTesting
    public GraphQLToAvroConverter(final String namespace) {
        this.avroNamespace = namespace;
    }

    @Override
    public ParsedSchema convert(final String graphQLSchema) {
        final Schema schema = this.convertToSchema(graphQLSchema);
        return new AvroSchema(schema);
    }

    /**
     * Converts a GraphQL Schema to an Avro Schema.
     *
     * @param schema GraphQL schema
     * @return Avro schema
     */
    private Schema convertToSchema(final String schema) {
        // first, we parse the schema and translate it into GraphQL object definition. Then, we can convert this
        // representation to an avro schema.
        // TODO: evaluate whether the GraphQL's SDLDefinition might be enough for translating to avro

        final GraphQLObjectType rootType = this.getRootTypeFromSchema(schema);
        final List<Schema.Field> fields = rootType.getFieldDefinitions().stream()
            .map(this::fromFieldDefinition)
            .collect(Collectors.toList());

        return Schema.createRecord(rootType.getName(), rootType.getDescription(), this.avroNamespace, false, fields);
    }

    /**
     * Translates a GraphQL field into an Avro field.
     *
     * @param fieldDefinition the GraphQL field
     * @return Avro field with same characteristics as the GraphQL field
     */
    private Schema.Field fromFieldDefinition(final GraphQLFieldDefinition fieldDefinition) {
        final String name = fieldDefinition.getName();
        final GraphQLType type = fieldDefinition.getType();
        final String description = fieldDefinition.getDescription();

        final Schema schema = this.getSchema(type);
        return new Schema.Field(name, schema, description);
    }

    /**
     * Converts a GraphQL type into an Avro schema.
     *
     * <p>
     * <b>Note</b>: Nullability is handled differently in GraphQL than in Avro. In GraphQL, everything is nullable by
     * default and a non-nullable field has to be specified explicitly. In avro, it is the other way around. We take
     * care of that here.
     *
     * @param type the GraphQL type to convert
     * @return Avro schema with the same characteristics as the GraphQL type
     */
    private Schema getSchema(final GraphQLType type) {
        if (isNonNull(type)) {
            return this.buildSchema(unwrapOne(type));
        }
        return Schema.createUnion(Schema.create(Schema.Type.NULL), this.buildSchema(type));
    }

    /**
     * Converts a GraphQL type into an Avro schema.
     *
     * <p>
     * This method assumes that all fields are nullable and therefore {@link graphql.schema.GraphQLNonNull} is invalid
     * as an argument. This method should only be called through {@link GraphQLToAvroConverter#getSchema(GraphQLType)}.
     *
     * @param type the GraphQL type to convert (GraphQLNonNull not allowed)
     * @return Avro schema with the same characteristics as the GraphQL type
     */
    private Schema buildSchema(final GraphQLType type) {
        if (type instanceof GraphQLObjectType) {
            return this.createObjectSchema((GraphQLObjectType) type);
        } else if (type instanceof GraphQLUnionType) {
            return this.createUnionSchema((GraphQLUnionType) type);
        } else if (isScalar(type)) {
            return createScalarSchema((GraphQLScalarType) type);
        } else if (isEnum(type)) {
            return this.createEnumSchema((GraphQLEnumType) type);
        } else if (isList(type)) {
            return this.createListSchema((GraphQLList) type);
        } else {
            throw new BadArgumentException(String.format("Type %s not recognized", GraphQLTypeUtil.simplePrint(type)));
        }
    }

    private Schema createUnionSchema(final GraphQLUnionType type) {
        final List<Schema> unionTypeSchemas = type.getTypes().stream()
            .map(this::getSchema)
            .collect(Collectors.toList());

        return Schema.createUnion(unionTypeSchemas);
    }

    private Schema createListSchema(final GraphQLList listType) {
        final Schema wrappedSchema = this.getSchema(listType.getWrappedType());
        return Schema.createArray(wrappedSchema);
    }

    private Schema createEnumSchema(final GraphQLEnumType enumType) {
        final List<String> values = enumType.getDefinition()
            .getEnumValueDefinitions()
            .stream()
            .map(EnumValueDefinition::getName)
            .collect(Collectors.toList());
        return Schema.createEnum(enumType.getName(), enumType.getDescription(), this.avroNamespace, values);
    }

    private Schema createObjectSchema(final GraphQLObjectType objectType) {
        final List<Schema.Field> fields = objectType.getFieldDefinitions()
            .stream()
            .map(this::fromFieldDefinition)
            .collect(Collectors.toList());
        final String name = objectType.getName();
        final String description = objectType.getDescription();
        return Schema.createRecord(name, description, this.avroNamespace, false, fields);
    }

    private static Schema createScalarSchema(final GraphQLScalarType scalarType) {
        final Schema.Type type = SCALAR_MAPPING.get(scalarType.getName());
        if (type == null) {
            final String message = String.format("Scalar %s not supported", GraphQLTypeUtil.simplePrint(scalarType));
            throw new BadArgumentException(message);
        }
        return Schema.create(type);
    }

    private static Map<String, Schema.Type> scalarTypeMap() {
        // Currently, not supported since no lossless/straight-forward conversion possible:
        // Map.entry(Scalars.GraphQLByte, Schema.Type.BYTES),
        // Map.entry(Scalars.GraphQLBigInteger, Schema.Type.LONG),
        // Map.entry(Scalars.GraphQLBigDecimal, Schema.Type.LONG),
        return Map.of(
            Scalars.GraphQLInt.getName(), Schema.Type.INT,
            Scalars.GraphQLFloat.getName(), Schema.Type.FLOAT,
            Scalars.GraphQLString.getName(), Schema.Type.STRING,
            Scalars.GraphQLBoolean.getName(), Schema.Type.BOOLEAN,
            Scalars.GraphQLID.getName(), Schema.Type.STRING,
            ExtendedScalars.GraphQLLong.getName(), Schema.Type.LONG,
            ExtendedScalars.GraphQLShort.getName(), Schema.Type.INT,
            ExtendedScalars.GraphQLChar.getName(), Schema.Type.STRING
        );
    }
}
