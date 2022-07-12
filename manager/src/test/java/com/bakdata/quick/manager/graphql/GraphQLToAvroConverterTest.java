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

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class GraphQLToAvroConverterTest {
    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "graphql");
    private final GraphQLToAvroConverter graphQLToAvroConverter = new GraphQLToAvroConverter("foo.bar.test.avro");

    @Test
    void shouldSetAvroNamespaceFromProperties() {
        assertThat(this.graphQLToAvroConverter.getAvroNamespace()).isEqualTo("foo.bar.test.avro");
    }

    @Test
    void shouldConvertGraphQLEnumFields(final TestInfo testInfo) throws IOException {
        final Schema parsedSchema = this.getSchema(testInfo);

        assertThat(parsedSchema.getField("status"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .extracting(Schema::getType)
            .containsExactly(Type.NULL, Type.ENUM);

        assertThat(parsedSchema.getField("status").schema().getTypes())
            .last()
            .satisfies(enumType -> assertThat(enumType.getName()).isEqualTo("Status"))
            .extracting(Schema::getEnumSymbols)
            .satisfies(symbols -> {
                assertThat(symbols).hasSize(2);
                assertThat(symbols).containsExactly("SOLD", "AVAILABLE");
            });
    }

    @Test
    void shouldConvertGraphQLObjectTypes(final TestInfo testInfo) throws IOException {
        final Schema parsedSchema = this.getSchema(testInfo);

        assertThat(parsedSchema.getField("id"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .extracting(Schema::getType)
            .contains(Type.NULL, Type.STRING);

        assertThat(parsedSchema.getField("complexObject"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types -> assertThat(types)
                .extracting(Schema::getType)
                .containsExactly(Type.NULL, Type.RECORD)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "ComplexObject")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(2)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("id", "nestedObject"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.NULL, Type.STRING, Type.NULL, Type.RECORD)
            );

        assertThat(parsedSchema.getField("complexObject"))
            .extracting(Field::schema)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .last(InstanceOfAssertFactories.type(Field.class))
            .extracting(Field::schema)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("id"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.NULL, Type.STRING)
            );
    }

    @Test
    void shouldConvertGraphQLScalarFields(final TestInfo testInfo) throws IOException {
        final Schema parsedSchema = this.getSchema(testInfo);

        final Map<String, Type> expectedTypeForField = Map.ofEntries(
            Map.entry("int", Type.INT),
            Map.entry("float", Type.FLOAT),
            Map.entry("string", Type.STRING),
            Map.entry("bool", Type.BOOLEAN),
            Map.entry("id", Type.STRING),
            Map.entry("long", Type.LONG),
            Map.entry("short", Type.INT),
            Map.entry("char", Type.STRING)
        );
        for (final Entry<String, Type> typeEntry : expectedTypeForField.entrySet()) {
            assertThat(parsedSchema.getField(typeEntry.getKey()))
                .isNotNull()
                .extracting(field -> field.schema().getType())
                .isEqualTo(typeEntry.getValue());
        }
    }

    @Test
    void shouldConvertListType(final TestInfo testInfo) throws IOException {
        final Schema parsedSchema = this.getSchema(testInfo);

        assertThat(parsedSchema.getField("optionalSimpleList"))
            .isNotNull()
            .extracting(Field::schema)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            ).last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.INT)
            );

        assertThat(parsedSchema.getField("optionalComplexList"))
            .isNotNull()
            .extracting(Field::schema)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            ).last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD)
            ).last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "ComplexObject")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(1)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("id"));

        assertThat(parsedSchema.getField("requiredSimpleList"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(field -> assertThat(field.getType()).isEqualTo(Type.ARRAY))
            .extracting(Schema::getElementType)
            .satisfies(items -> assertThat(items.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.INT)
            );

        assertThat(parsedSchema.getField("requiredComplexList"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(field -> assertThat(field.getType()).isEqualTo(Type.ARRAY))
            .extracting(Schema::getElementType)
            .satisfies(items -> assertThat(items.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD)
            ).last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "ComplexObject")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(1)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("id"));

        assertThat(parsedSchema.getField("requiredComplexList2"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(field -> assertThat(field.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes)
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            );

        assertThat(parsedSchema.getField("requiredComplexList3"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(field -> assertThat(field.getType()).isEqualTo(Type.ARRAY))
            .extracting(Schema::getElementType)
            .satisfies(items -> assertThat(items.getType()).isEqualTo(Type.RECORD));
    }

    @Test
    void shouldConvertOptionalAndRequired(final TestInfo testInfo) throws IOException {
        final Schema parsedSchema = this.getSchema(testInfo);

        assertThat(parsedSchema.getField("required"))
            .isNotNull()
            .extracting(field -> field.schema().getType())
            .isEqualTo(Type.INT);

        assertThat(parsedSchema.getField("optional"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, list(Schema.class))
            .satisfies(types -> assertThat(types).extracting(Schema::getType).containsExactly(Type.NULL, Type.INT));
    }

    private Schema getSchema(final TestInfo testInfo) throws IOException {
        final String graphQLSchema =
            Files.readString(workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql"));
        AvroSchema parsedSchema = (AvroSchema) this.graphQLToAvroConverter.convert(graphQLSchema);
        return parsedSchema.rawSchema();
    }

    private static List<Type> unwrapSchemaType(final Schema schema) {
        switch (schema.getType()) {
            case ARRAY:
                return unwrapSchemaType(schema.getElementType());
            case UNION:
                return schema.getTypes().stream()
                    .flatMap(childSchema -> unwrapSchemaType(childSchema).stream())
                    .collect(Collectors.toList());
            default:
                return List.of(schema.getType());
        }
    }
}
