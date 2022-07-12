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

import com.bakdata.quick.common.condition.ProtobufSchemaFormatCondition;
import com.bakdata.quick.common.config.ProtobufConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.exception.InternalErrorException;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import graphql.Scalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.micronaut.context.annotation.Requires;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Converts a GraphQL Schema to a Protobuf Schema.
 */
@Singleton
@Requires(condition = ProtobufSchemaFormatCondition.class)
public class GraphQLToProtobufConverter implements GraphQLConverter {

    private static final String SYNTAX = "proto3";
    private static final String PROTO_FILE_EXTENSION = ".proto";
    @Getter
    private final String protobufPackage;

    private static final Map<GraphQLScalarType, FieldDescriptorProto.Type> SCALAR_MAPPING = scalarTypeMap();

    @Inject
    public GraphQLToProtobufConverter(final ProtobufConfig protobufConfig) {
        this.protobufPackage = protobufConfig.getProtobufPackage();
    }

    @Override
    public ParsedSchema convert(final String graphQLSchema) {
        try {
            final Descriptor descriptor = this.convertToDescriptor(graphQLSchema);
            return new ProtobufSchema(descriptor);
        } catch (final DescriptorValidationException exception) {
            throw new InternalErrorException(
                String.format(exception.getMessage(),
                    "Something went wrong on our side! The protobuf message can not be converted: %s"));
        }
    }

    private Descriptor convertToDescriptor(final String schema) throws DescriptorValidationException {
        final GraphQLObjectType rootType = this.getRootTypeFromSchema(schema);

        final FileDescriptorProto.Builder file =
            DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .setSyntax(SYNTAX)
                .setPackage(this.protobufPackage)
                .setName(rootType.getName() + PROTO_FILE_EXTENSION);

        generateFile(rootType.getName(), rootType.getFieldDefinitions(), file);

        final Descriptors.FileDescriptor fileDescriptor =
            Descriptors.FileDescriptor.buildFrom(file.build(), new Descriptors.FileDescriptor[] {});

        return fileDescriptor.findMessageTypeByName(rootType.getName());
    }

    /**
     * Iterates over all the fields in GraphQL Type and creates a Protobuf message that is appended to the protobuf
     * file.
     *
     * @param messageName name of the protobuf message.
     * @param fieldDefinitions Fields of the GraphQL Object type.
     * @param fileBuilder The Protobuf FileDescriptor builder that contains the converted messages.
     */
    private static void generateFile(
        final String messageName,
        final List<GraphQLFieldDefinition> fieldDefinitions,
        final FileDescriptorProto.Builder fileBuilder) {

        final DescriptorProto.Builder currentMessage = DescriptorProto.newBuilder().setName(messageName);

        for (int index = 0; index < fieldDefinitions.size(); index++) {

            final GraphQLFieldDefinition graphQLFieldDefinition = fieldDefinitions.get(index);
            final GraphQLOutputType graphQLType = graphQLFieldDefinition.getType();

            final Label label = isNonNull(graphQLType) ? Label.LABEL_REQUIRED : Label.LABEL_OPTIONAL;
            final GraphQLType unwrappedType = isNonNull(graphQLType) ? unwrapOne(graphQLType) : graphQLType;

            createMessage(fileBuilder,
                currentMessage,
                graphQLFieldDefinition,
                unwrappedType,
                index + 1,
                label);
        }

        fileBuilder.addMessageType(currentMessage);
    }

    private static void createMessage(
        final FileDescriptorProto.Builder fileBuilder,
        final DescriptorProto.Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final GraphQLType graphQLType,
        final int fieldNumber,
        final Label label) {
        if (graphQLType instanceof GraphQLObjectType) {
            handleObjectType(fileBuilder, currentMessage, graphQLFieldDefinition, fieldNumber, label,
                (GraphQLObjectType) graphQLType);

        } else if (isScalar(graphQLType)) {
            final FieldDescriptorProto scalarFieldBuilder =
                createFieldDescriptorForScalarType((GraphQLScalarType) graphQLType,
                    graphQLFieldDefinition.getName(),
                    fieldNumber,
                    label);
            currentMessage.addField(scalarFieldBuilder);

        } else if (isEnum(graphQLType)) {
            handleEnumType(fileBuilder, currentMessage, graphQLFieldDefinition, fieldNumber, label,
                (GraphQLEnumType) graphQLType);
        } else if (isList(graphQLType)) {
            handleListType(fileBuilder, currentMessage, graphQLFieldDefinition, (GraphQLList) graphQLType, fieldNumber);
        } else {
            throw new BadArgumentException(
                String.format("Type %s not recognized", GraphQLTypeUtil.simplePrint(graphQLType)));
        }
    }

    /**
     * Adds a field of type to the message and then calls the generateFile function again to unwrap the object type and
     * create a new message. If the message already exists in the file it will return.
     */
    private static void handleObjectType(
        final FileDescriptorProto.Builder fileBuilder,
        final DescriptorProto.Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final int fieldNumber,
        final Label label,
        final GraphQLObjectType graphQLObjectType) {
        currentMessage.addField(createFieldWithType(graphQLFieldDefinition.getName(),
            fieldNumber,
            FieldDescriptorProto.Type.TYPE_MESSAGE,
            graphQLObjectType.getName(),
            label));

        if (fileBuilder.getMessageTypeList().stream()
            .anyMatch(message -> message.getName().equals(graphQLObjectType.getName()))) {
            return;
        }

        generateFile(graphQLObjectType.getName(), graphQLObjectType.getFieldDefinitions(), fileBuilder);
    }

    /**
     * Creates a FieldDescriptorProto object for a specific type. These fields could be either a type of TYPE_MESSAGE or
     * TYPE_ENUM. If a FieldDescriptorProto object type is one of these the property TypeName must be specified
     * otherwise a {@link DescriptorValidationException} is thrown.
     *
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type#TYPE_MESSAGE
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type#TYPE_ENUM
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Builder#setTypeName(String)
     */
    private static FieldDescriptorProto createFieldWithType(
        final String fieldName,
        final int fieldNumber,
        final FieldDescriptorProto.Type type,
        final String typeName,
        final Label label) {

        return FieldDescriptorProto.newBuilder()
            .setName(fieldName)
            .setType(type)
            .setTypeName(typeName)
            .setNumber(fieldNumber)
            .setLabel(label)
            .build();
    }

    /**
     * Creates a {@link FieldDescriptorProto} object from a scalar GraphQL FieldDescriptorProto.Type.
     */
    private static FieldDescriptorProto createFieldDescriptorForScalarType(
        final GraphQLScalarType graphQLScalarType,
        final String fieldName,
        final int fieldNumber,
        final Label label) {

        final FieldDescriptorProto.Type protoType = SCALAR_MAPPING.get(graphQLScalarType);
        if (protoType == null) {
            final String message =
                String.format("Scalar %s not supported", GraphQLTypeUtil.simplePrint(graphQLScalarType));
            throw new BadArgumentException(message);
        }

        return FieldDescriptorProto.newBuilder()
            .setName(fieldName)
            .setType(protoType)
            .setNumber(fieldNumber)
            .setLabel(label)
            .build();
    }

    /**
     * Sets the field to a type of Enum and adds an {@link EnumDescriptorProto} to the file builder.
     */
    private static void handleEnumType(
        final FileDescriptorProto.Builder fileBuilder,
        final DescriptorProto.Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final int fieldNumber,
        final Label label,
        final GraphQLEnumType graphQLEnumType) {

        currentMessage.addField(createFieldWithType(graphQLFieldDefinition.getName(),
            fieldNumber,
            FieldDescriptorProto.Type.TYPE_ENUM,
            graphQLEnumType.getName(),
            label));

        final EnumDescriptorProto enumDescriptor = createEnumDescriptor(graphQLEnumType);
        if (!fileBuilder.getEnumTypeList().contains(enumDescriptor)) {
            fileBuilder.addEnumType(enumDescriptor);
        }
    }

    /**
     * Creates a {@link EnumDescriptorProto} object from a scalar GraphQL type. For example the GraphQL enum with
     * these fields:
     * <pre>
     * enum Foo {
     *   A, B
     * }
     * </pre>
     * Will be transformed to a protobuf enum with these fields:
     * <pre>
     * enum Foo {
     *   FOO_UNSPECIFIED = 0;
     *   FOO_A = 1;
     *   FOO_B = 2;
     * }
     * </pre>
     * For more information regarding protobuf enums visit the <a
     * href="https://docs.buf.build/best-practices/style-guide#enums">buf style-guide</a>
     */
    private static EnumDescriptorProto createEnumDescriptor(final GraphQLEnumType enumType) {
        final String unspecifiedField = generateEnumFieldName(enumType.getName(), "UNSPECIFIED");
        final List<EnumValueDescriptorProto> values = enumType.getDefinition()
            .getEnumValueDefinitions()
            .stream()
            .map(enumValueDefinition -> EnumValueDescriptorProto.newBuilder()
                .setName(generateEnumFieldName(enumType.getName(), enumValueDefinition.getName())).build())
            .collect(Collectors.toList());

        return EnumDescriptorProto.newBuilder()
            .setName(enumType.getName())
            .addValue(0, EnumValueDescriptorProto.newBuilder().setName(unspecifiedField).build())
            .addAllValue(values)
            .build();
    }

    /**
     * Generates the enum field name by prefixing the uppercase type name to the field name.
     *
     * @param typeName prefix to the field name
     * @param enumValueDefinitionName the graphQL enum field name
     * @return enum field name
     */
    private static String generateEnumFieldName(final String typeName, final String enumValueDefinitionName) {
        return typeName.toUpperCase() + "_" + enumValueDefinitionName.toUpperCase();
    }

    /**
     * Handles GraphQL list type. Gets the wrapped type and check the nullability  of the field. It then crates the
     * message.
     */
    private static void handleListType(
        final FileDescriptorProto.Builder fileBuilder,
        final DescriptorProto.Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final GraphQLList graphQLList,
        final int fieldNumber) {

        final GraphQLType wrappedType = graphQLList.getWrappedType();
        final GraphQLType unwrappedType = isNonNull(wrappedType) ? unwrapOne(wrappedType) : wrappedType;

        createMessage(fileBuilder,
            currentMessage,
            graphQLFieldDefinition,
            unwrappedType,
            fieldNumber,
            Label.LABEL_REPEATED);
    }

    private static Map<GraphQLScalarType, FieldDescriptorProto.Type> scalarTypeMap() {
        return Map.of(
            Scalars.GraphQLInt, FieldDescriptorProto.Type.TYPE_INT32,
            Scalars.GraphQLFloat, FieldDescriptorProto.Type.TYPE_FLOAT,
            Scalars.GraphQLString, FieldDescriptorProto.Type.TYPE_STRING,
            Scalars.GraphQLBoolean, FieldDescriptorProto.Type.TYPE_BOOL,
            Scalars.GraphQLID, FieldDescriptorProto.Type.TYPE_STRING,
            Scalars.GraphQLLong, FieldDescriptorProto.Type.TYPE_INT64,
            Scalars.GraphQLShort, FieldDescriptorProto.Type.TYPE_INT32,
            Scalars.GraphQLChar, FieldDescriptorProto.Type.TYPE_STRING
        );
    }
}
