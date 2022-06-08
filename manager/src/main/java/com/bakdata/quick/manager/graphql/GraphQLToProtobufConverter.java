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

import com.bakdata.quick.common.config.ProtobufConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto.Builder;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Converts a GraphQL Schema to a Protobuf Schema.
 */
@Singleton
public class GraphQLToProtobufConverter implements GraphQLConverter {

    @Getter
    private final String protobufPackage;

    private static final Map<GraphQLScalarType, Type> SCALAR_MAPPING = scalarTypeMap();

    @Inject
    public GraphQLToProtobufConverter(final ProtobufConfig protobufConfig) {
        this(protobufConfig.getProtobufPackage());
    }

    @VisibleForTesting
    public GraphQLToProtobufConverter(final String protobufPackage) {
        this.protobufPackage = protobufPackage;
    }

    @Override
    public ParsedSchema convert(final String graphQLSchema) {
        try {
            final Descriptor descriptor = this.convertToDescriptor(graphQLSchema);
            return new ProtobufSchema(descriptor);
        } catch (final DescriptorValidationException exception) {
            throw new BadRequestException(
                String.format(exception.getMessage(), "The protobuf message can not be converted %s"));
        }
    }

    private Descriptor convertToDescriptor(final String schema) throws DescriptorValidationException {
        final GraphQLObjectType rootType = this.getRootTypeFromSchema(schema);

        final FileDescriptorProto.Builder file =
            DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .setSyntax("proto3")
                .setPackage(this.protobufPackage)
                .setName(rootType.getName() + ".proto");

        generateFile(rootType.getName(), rootType.getFieldDefinitions(), file);

        final Descriptors.FileDescriptor fileDescriptor =
            Descriptors.FileDescriptor.buildFrom(file.build(), new Descriptors.FileDescriptor[] {});

        return fileDescriptor.findMessageTypeByName(rootType.getName());
    }

    /**
     * This function iterates over all the fields in GraphQL Type and creates a Protobuf message that is appended to the
     * protobuf file.
     *
     * @param messageName name of the protobuf message.
     * @param fieldDefinitions Fields of the GraphQL Object type.
     * @param fileBuilder The Protobuf FileDescriptor builder that contains the converted messages.
     */
    private static void generateFile(
        final String messageName,
        final List<GraphQLFieldDefinition> fieldDefinitions,
        final FileDescriptorProto.Builder fileBuilder) {

        final Builder currentMessage = DescriptorProto.newBuilder().setName(messageName);

        for (int index = 0; index < fieldDefinitions.size(); index++) {

            final GraphQLFieldDefinition graphQLFieldDefinition = fieldDefinitions.get(index);
            final GraphQLOutputType graphQLType = graphQLFieldDefinition.getType();

            final int fieldNumber = index + 1;

            if (isNonNull(graphQLType)) {
                createMessage(fileBuilder,
                    currentMessage,
                    graphQLFieldDefinition,
                    unwrapOne(graphQLType),
                    fieldNumber,
                    Label.LABEL_REQUIRED);
            } else {
                createMessage(fileBuilder,
                    currentMessage,
                    graphQLFieldDefinition,
                    graphQLType,
                    fieldNumber,
                    Label.LABEL_OPTIONAL);
            }
        }
        fileBuilder.addMessageType(currentMessage);
    }

    private static void createMessage(final FileDescriptorProto.Builder fileBuilder,
        final Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final GraphQLType graphQLType,
        final int fieldNumber,
        final Label label) {
        if (graphQLType instanceof GraphQLObjectType) {
            handleObjectType(fileBuilder, currentMessage, graphQLFieldDefinition, fieldNumber, label,
                (GraphQLObjectType) graphQLType);

        } else if (isScalar(graphQLType)) {
            final FieldDescriptorProto scalarFieldBuilder =
                createFieldDescriptor((GraphQLScalarType) graphQLType,
                    graphQLFieldDefinition.getName(),
                    fieldNumber,
                    label);
            currentMessage.addField(scalarFieldBuilder);

        } else if (isEnum(graphQLType)) {
            handleEnumType(fileBuilder, currentMessage, graphQLFieldDefinition, fieldNumber, label,
                (GraphQLEnumType) graphQLType);
        } else if (isList(graphQLType)) {
            final GraphQLList graphQLList = (GraphQLList) graphQLType;

            createMessage(fileBuilder,
                currentMessage,
                graphQLFieldDefinition,
                graphQLList.getWrappedType(),
                fieldNumber,
                Label.LABEL_REPEATED);
        } else {
            throw new BadArgumentException(
                String.format("Type %s not recognized", GraphQLTypeUtil.simplePrint(graphQLType)));
        }
    }

    private static void handleObjectType(
        final FileDescriptorProto.Builder fileBuilder,
        final Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final int fieldNumber,
        final Label label,
        final GraphQLObjectType graphQLObjectType) {
        currentMessage.addField(buildFieldWithType(graphQLFieldDefinition.getName(),
            fieldNumber,
            Type.TYPE_MESSAGE,
            graphQLObjectType.getName(),
            label));

        final Builder descriptorProtoBuilder = DescriptorProto.newBuilder();
        descriptorProtoBuilder.setName(graphQLObjectType.getName());

        final FileDescriptorProto.Builder tempFile = FileDescriptorProto.newBuilder();

        generateFile(graphQLObjectType.getName(), graphQLObjectType.getFieldDefinitions(), tempFile);

        final List<DescriptorProto> collect = tempFile.getMessageTypeList().stream()
            .filter(message -> !fileBuilder.getMessageTypeList().contains(message)).collect(Collectors.toList());
        fileBuilder.addAllMessageType(collect);
    }

    /**
     * This function creates a FieldDescriptorProto object for a specific type. These fields could be either a type of
     * TYPE_MESSAGE or TYPE_ENUM. If a FieldDescriptorProto object type is one of these the property TypeName must be
     * specified otherwise a {@link DescriptorValidationException} is thrown.
     *
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type#TYPE_MESSAGE
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type#TYPE_ENUM
     * @see com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Builder#setTypeName(String)
     */
    @NotNull
    private static FieldDescriptorProto buildFieldWithType(
        final String fieldName,
        final int fieldNumber,
        final Type type,
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
     * This function creates a FieldDescriptorProto object from a scalar GraphQL type.
     */
    @NotNull
    private static FieldDescriptorProto createFieldDescriptor(
        final GraphQLScalarType graphQLScalarType,
        final String fieldName,
        final int fieldNumber,
        final Label label) {

        final Type protoType = SCALAR_MAPPING.get(graphQLScalarType);
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

    private static void handleEnumType(
        final FileDescriptorProto.Builder fileBuilder,
        final Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final int fieldNumber,
        final Label label,
        final GraphQLEnumType graphQLEnumType) {

        currentMessage.addField(buildFieldWithType(graphQLFieldDefinition.getName(),
            fieldNumber,
            Type.TYPE_ENUM,
            graphQLEnumType.getName(),
            label));

        final EnumDescriptorProto enumSchema = createEnumDescriptor(graphQLEnumType);
        fileBuilder.addEnumType(enumSchema);
    }

    private static EnumDescriptorProto createEnumDescriptor(final GraphQLEnumType enumType) {
        final List<EnumValueDescriptorProto> values = enumType.getDefinition()
            .getEnumValueDefinitions()
            .stream()
            .map(enumValueDefinition -> EnumValueDescriptorProto.newBuilder()
                .setName(enumValueDefinition.getName()).build())
            .collect(Collectors.toList());

        return EnumDescriptorProto.newBuilder()
            .setName(enumType.getName())
            .addAllValue(values)
            .build();
    }

    private static Map<GraphQLScalarType, Type> scalarTypeMap() {
        return Map.ofEntries(
            Map.entry(Scalars.GraphQLInt, Type.TYPE_INT32),
            Map.entry(Scalars.GraphQLFloat, Type.TYPE_FLOAT),
            Map.entry(Scalars.GraphQLString, Type.TYPE_STRING),
            Map.entry(Scalars.GraphQLBoolean, Type.TYPE_BOOL),
            Map.entry(Scalars.GraphQLID, Type.TYPE_STRING),
            Map.entry(Scalars.GraphQLLong, Type.TYPE_INT64),
            Map.entry(Scalars.GraphQLShort, Type.TYPE_INT32),
            Map.entry(Scalars.GraphQLChar, Type.TYPE_STRING)
        );
    }
}
