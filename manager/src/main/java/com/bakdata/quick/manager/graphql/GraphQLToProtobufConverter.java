package com.bakdata.quick.manager.graphql;

import com.bakdata.quick.common.config.ProtobufConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.DescriptorProtos.DescriptorProto.Builder;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import graphql.Scalars;
import graphql.schema.*;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.schema.GraphQLTypeUtil.*;

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
    public ParsedSchema convert(String graphQLSchema) {
        try {
            Descriptor descriptor = this.convertToDescriptor(graphQLSchema);
            return new ProtobufSchema(descriptor);
        } catch (DescriptorValidationException e) {
            throw new BadRequestException();
        }
    }

    public Descriptor convertToDescriptor(final String schema) throws DescriptorValidationException {
        GraphQLObjectType rootType = this.getRootTypeFromSchema(schema);

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
                extracted(fileBuilder, currentMessage, graphQLFieldDefinition, unwrapOne(graphQLType), fieldNumber,
                    Label.LABEL_REQUIRED);
            } else {
                extracted(fileBuilder, currentMessage, graphQLFieldDefinition, graphQLType, fieldNumber,
                    Label.LABEL_OPTIONAL);
            }
        }
        fileBuilder.addMessageType(currentMessage);
    }

    private static void extracted(final FileDescriptorProto.Builder fileBuilder,
        final Builder currentMessage,
        final GraphQLFieldDefinition graphQLFieldDefinition,
        final GraphQLType graphQLType,
        final int fieldNumber,
        final Label label) {
        if (graphQLType instanceof GraphQLObjectType) {
            final GraphQLObjectType graphQLObjectType = (GraphQLObjectType) graphQLType;

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

        } else if (isScalar(graphQLType)) {
            final FieldDescriptorProto scalarFieldBuilder =
                createFieldDescriptor((GraphQLScalarType) graphQLType,
                    graphQLFieldDefinition.getName(),
                    fieldNumber,
                    label);

            currentMessage.addField(scalarFieldBuilder);

        } else if (isEnum(graphQLType)) {
            final GraphQLEnumType graphQLEnumType = (GraphQLEnumType) graphQLType;

            currentMessage.addField(buildFieldWithType(graphQLFieldDefinition.getName(),
                fieldNumber,
                Type.TYPE_ENUM,
                graphQLEnumType.getName(),
                label));

            final EnumDescriptorProto enumSchema = createEnumDescriptor(graphQLEnumType);
            fileBuilder.addEnumType(enumSchema);
        } else if (isList(graphQLType)) {
            final GraphQLList graphQLList = (GraphQLList) graphQLType;

            extracted(fileBuilder,
                currentMessage,
                graphQLFieldDefinition,
                graphQLList.getWrappedType(),
                fieldNumber,
                Label.LABEL_REPEATED);
        }
    }

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

    @NotNull
    private static FieldDescriptorProto createFieldDescriptor(final GraphQLScalarType graphQLScalarType,
        final String fieldName,
        final int fieldNumber, final Label label) {

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
