package com.bakdata.quick.manager.graphql;

import static graphql.schema.GraphQLTypeUtil.isEnum;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.schema.GraphQLTypeUtil.isScalar;
import static graphql.schema.GraphQLTypeUtil.unwrapOne;

import com.bakdata.quick.common.config.ProtobufConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.graphql.GraphQLUtils;
import com.bakdata.quick.common.type.QuickTopicType;
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
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Singleton
public class GraphQLToProtobufConverter {

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

    public Descriptor convertToSchema(final String schema) throws DescriptorValidationException {
        // TODO move to base class
        // extending the schema with an empty query type is necessary because parsing fails otherwise
        final SchemaParser schemaParser = new SchemaParser();
        final TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
        final String rootTypeName = GraphQLUtils.getRootType(QuickTopicType.SCHEMA, typeDefinitionRegistry);

        // existence required for building a GraphQLSchema, no wiring needed otherwise
        final RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        final GraphQLObjectType rootType = graphQLSchema.getObjectType(rootTypeName);

        final FileDescriptorProto.Builder file =
            DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .setSyntax("proto3")
                .setPackage(this.protobufPackage)
                .setName(rootTypeName + ".proto");

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
