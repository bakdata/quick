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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import com.bakdata.quick.common.config.ProtobufConfig;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class GraphQLToProtobufConverterTest {
    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "graphql");

    private final GraphQLToProtobufConverter graphQLToProtobufConverter =
        new GraphQLToProtobufConverter(new ProtobufConfig("foo.bar.test.v1"));

    @Test
    void shouldSetProtobufPackageFromProperties() {
        assertThat(this.graphQLToProtobufConverter.getProtobufPackage()).isEqualTo("foo.bar.test.v1");
    }

    @Test
    void shouldConvertGraphQLEnumFields(final TestInfo testInfo) throws IOException {
        final FileDescriptor file = this.getFileDescriptor(testInfo);

        assertThat(file.getMessageTypes()).hasSize(1);
        assertThat(file.findMessageTypeByName("Product"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(2)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors).
                extracting(FieldDescriptor::getName)
                .containsExactly("status", "status2"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getType)
                .containsExactly(FieldDescriptor.Type.ENUM, FieldDescriptor.Type.ENUM));

        assertThat(file.getEnumTypes()).hasSize(1);
        assertThat(file.findEnumTypeByName("Status")).isNotNull()
            .extracting(EnumDescriptor::getValues, list(EnumValueDescriptor.class))
            .hasSize(3)
            .satisfies(values -> assertThat(values).extracting(EnumValueDescriptor::getName)
                .containsExactly("STATUS_UNSPECIFIED", "STATUS_SOLD", "STATUS_AVAILABLE"));
    }

    @Test
    void shouldConvertGraphQLObjectTypes(final TestInfo testInfo) throws IOException {
        final FileDescriptor file = this.getFileDescriptor(testInfo);

        assertThat(file.getMessageTypes()).hasSize(3);

        assertThat(file.findMessageTypeByName("Mock"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(3)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getName).containsExactly("id", "complexObject", "simpleString"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getType)
                .containsExactly(FieldDescriptor.Type.STRING,
                    FieldDescriptor.Type.MESSAGE,
                    FieldDescriptor.Type.STRING));

        assertThat(file.findMessageTypeByName("ComplexObject"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(2)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getName).containsExactly("id", "nestedObject"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getType)
                .containsExactly(FieldDescriptor.Type.STRING,
                    FieldDescriptor.Type.MESSAGE));

        assertThat(file.findMessageTypeByName("NestedObject"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(1)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getName).containsExactly("id"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getType)
                .containsExactly(FieldDescriptor.Type.STRING));
    }

    @Test
    void shouldConvertGraphQLScalarFields(final TestInfo testInfo) throws IOException {
        final FileDescriptor file = this.getFileDescriptor(testInfo);

        assertThat(file.findMessageTypeByName("Scalars"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(8)
            .extracting(FieldDescriptor::getType)
            .containsExactly(FieldDescriptor.Type.INT32,
                FieldDescriptor.Type.FLOAT,
                FieldDescriptor.Type.STRING,
                FieldDescriptor.Type.BOOL,
                FieldDescriptor.Type.STRING,
                FieldDescriptor.Type.INT64,
                FieldDescriptor.Type.INT32,
                FieldDescriptor.Type.STRING);
    }

    @Test
    void shouldConvertOptionalAndRequired(final TestInfo testInfo) throws IOException {
        final FileDescriptor file = this.getFileDescriptor(testInfo);

        assertThat(file.findMessageTypeByName("OptionalRequired"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(2)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors).extracting(FieldDescriptor::getName)
                .containsExactly("required", "optional"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors).extracting(FieldDescriptor::isRequired)
                .containsExactly(true, false));
    }

    @Test
    void shouldConvertListType(final TestInfo testInfo) throws IOException {
        final FileDescriptor file = this.getFileDescriptor(testInfo);

        assertThat(file.getMessageTypes()).hasSize(2);

        assertThat(file.findMessageTypeByName("ListType"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(8)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getName)
                .containsExactly("optionalSimpleList", "optionalComplexList", "requiredSimpleList",
                    "requiredComplexList", "requiredSimpleList2", "requiredComplexList2", "requiredSimpleList3",
                    "requiredComplexList3"))
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors).extracting(FieldDescriptor::isRepeated)
                .containsExactly(true, true, true, true, true, true, true, true));

        assertThat(file.findMessageTypeByName("ComplexObject"))
            .isNotNull()
            .extracting(Descriptor::getFields, list(FieldDescriptor.class))
            .hasSize(1)
            .satisfies(fieldDescriptors -> assertThat(fieldDescriptors)
                .extracting(FieldDescriptor::getName)
                .containsExactly("id"))
            .satisfies(fieldDescriptor -> assertThat(fieldDescriptor)
                .extracting(FieldDescriptor::getType)
                .containsExactly(FieldDescriptor.Type.STRING)
            );
    }

    private FileDescriptor getFileDescriptor(final TestInfo testInfo) throws IOException {
        final String graphQLSchema =
            Files.readString(workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql"));
        ProtobufSchema protobufSchema = (ProtobufSchema) this.graphQLToProtobufConverter.convert(graphQLSchema);
        return protobufSchema.toDescriptor().getFile();
    }
}
