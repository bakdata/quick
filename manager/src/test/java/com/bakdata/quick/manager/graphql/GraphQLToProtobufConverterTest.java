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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphQLToProtobufConverterTest {
    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "graphql");

    private final GraphQLToProtobufConverter graphQLToProtobufConverter =
            new GraphQLToProtobufConverter("foo.bar.test.v1");

    @Test
    void shouldSetProtobufPackageFromProperties() {
        assertThat(this.graphQLToProtobufConverter.getProtobufPackage()).isEqualTo("foo.bar.test.v1");
    }

    @Test
    void shouldConvertGraphQLObjectTypes(final TestInfo testInfo) throws IOException {
        final Descriptor parsedSchema = this.getFileDescriptorProto(testInfo);
        final FileDescriptor file = parsedSchema.getFile();

        final List<Descriptor> messageTypes = file.getMessageTypes();
        assertThat(messageTypes.size()).isEqualTo(3);

        assertThat(messageTypes.get(2)).satisfies(rootMessage -> {
            assertThat(rootMessage.getName()).isEqualTo("Mock");
            assertThat(rootMessage.getFields().size()).isEqualTo(3);

            final FieldDescriptor complexField = rootMessage.getFields().get(1);
            assertThat(complexField.getName()).isEqualTo("complexObject");
            assertThat(complexField.getType().toProto()).isEqualTo(Type.TYPE_MESSAGE);
        });

        assertThat(messageTypes.get(1)).satisfies(complexMessage -> {
            assertThat(complexMessage.getName()).isEqualTo("ComplexObject");
            assertThat(complexMessage.getFields().size()).isEqualTo(2);

            final FieldDescriptor complexField = complexMessage.getFields().get(1);
            assertThat(complexField.getName()).isEqualTo("nestedObject");
            assertThat(complexField.getType().toProto()).isEqualTo(Type.TYPE_MESSAGE);
        });

        assertThat(messageTypes.get(0)).satisfies(complexMessage -> {
            assertThat(complexMessage.getName()).isEqualTo("NestedObject");
            assertThat(complexMessage.getFields().size()).isEqualTo(1);

            final FieldDescriptor complexField = complexMessage.getFields().get(0);
            assertThat(complexField.getName()).isEqualTo("id");
            assertThat(complexField.getType().toProto()).isEqualTo(Type.TYPE_STRING);
        });
    }

    @Test
    void shouldConvertGraphQLEnumFields(final TestInfo testInfo) throws IOException {
        final Descriptor parsedSchema = this.getFileDescriptorProto(testInfo);
        final FileDescriptor file = parsedSchema.getFile();

        assertThat(file.getEnumTypes().size()).isEqualTo(1);

        assertThat(file.getEnumTypes().get(0)).satisfies(statusEnum -> {
            assertThat(statusEnum.getName()).isEqualTo("Status");
            assertThat(statusEnum.getValues().size()).isEqualTo(2);
            assertThat(statusEnum.getValues().get(0).getName()).isEqualTo("STATUS_SOLD");
            assertThat(statusEnum.getValues().get(1).getName()).isEqualTo("STATUS_AVAILABLE");
        });
    }

    @Test
    void shouldConvertGraphQLScalarFields(final TestInfo testInfo) throws IOException {
        final Descriptor parsedSchema = this.getFileDescriptorProto(testInfo);

        final List<FieldDescriptorProto> expectedFieldDescriptorList = List.of(
                FieldDescriptorProto.newBuilder().setName("int").setType(Type.TYPE_INT32).build(),
                FieldDescriptorProto.newBuilder().setName("float").setType(Type.TYPE_FLOAT).build(),
                FieldDescriptorProto.newBuilder().setName("string").setType(Type.TYPE_STRING).build(),
                FieldDescriptorProto.newBuilder().setName("bool").setType(Type.TYPE_BOOL).build(),
                FieldDescriptorProto.newBuilder().setName("id").setType(Type.TYPE_STRING).build(),
                FieldDescriptorProto.newBuilder().setName("long").setType(Type.TYPE_INT64).build(),
                FieldDescriptorProto.newBuilder().setName("short").setType(Type.TYPE_INT32).build(),
                FieldDescriptorProto.newBuilder().setName("char").setType(Type.TYPE_STRING).build()
        );

        assertThat(parsedSchema.getFields().size()).isEqualTo(8);

        for (int index = 0; index < expectedFieldDescriptorList.size(); index++) {
            final FieldDescriptorProto fieldDescriptorProto = parsedSchema.toProto().getField(index);
            assertThat(fieldDescriptorProto.getType()).isEqualTo(expectedFieldDescriptorList.get(index).getType());
        }
    }

    @Test
    void shouldConvertOptionalAndRequired(final TestInfo testInfo) throws IOException {
        final Descriptor parsedSchema = this.getFileDescriptorProto(testInfo);
        assertThat(parsedSchema.getFields().size()).isEqualTo(2);

        assertThat(parsedSchema.getFields().get(0)).satisfies(requiredField -> {
            assertThat(requiredField.getName()).isEqualTo("required");
            assertThat(requiredField.isRequired()).isTrue();
        });

        assertThat(parsedSchema.getFields().get(1)).satisfies(requiredField -> {
            assertThat(requiredField.getName()).isEqualTo("optional");
            assertThat(requiredField.isRequired()).isFalse();
        });
    }

    @Test
    void shouldConvertListType(final TestInfo testInfo) throws IOException {
        final Descriptor parsedSchema = this.getFileDescriptorProto(testInfo);

        final FileDescriptor file = parsedSchema.getFile();

        final List<Descriptor> messageTypes = file.getMessageTypes();
        assertThat(messageTypes.size()).isEqualTo(3);

        final List<FieldDescriptor> fields = messageTypes.get(2).getFields();
        assertThat(fields.size()).isEqualTo(6);

        assertThat(fields.get(0)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("simpleList");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });

        assertThat(fields.get(1)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("complexList");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });

        assertThat(fields.get(2)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("requiredSimpleList");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });

        assertThat(fields.get(3)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("requiredComplexList");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });

        assertThat(fields.get(4)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("optionalComplexList2");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });

        assertThat(fields.get(5)).satisfies(fieldDescriptor -> {
            assertThat(fieldDescriptor.getName()).isEqualTo("requiredComplexList2");
            assertThat(fieldDescriptor.isRepeated()).isTrue();
        });
    }

    private Descriptor getFileDescriptorProto(final TestInfo testInfo) throws IOException {
        final String allScalarsSchema =
                Files.readString(workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql"));
        ProtobufSchema protobufSchema = (ProtobufSchema) this.graphQLToProtobufConverter.convert(allScalarsSchema);
        return protobufSchema.toDescriptor();
    }
}
