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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GraphQLToAvroConverterTest {
    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "graphql");

    private static String productSchema = null;
    private static String contractSchema = null;
    private static String scalarSchema = null;

    private final GraphQLToAvroConverter graphQLToAvroConverter = new GraphQLToAvroConverter("foo.bar.test.avro");

    @BeforeAll
    static void beforeAll() throws IOException {
        productSchema = Files.readString(workingDirectory.resolve("product.graphql"));
        contractSchema = Files.readString(workingDirectory.resolve("contract.graphql"));
        scalarSchema = Files.readString(workingDirectory.resolve("allScalars.graphql"));
    }

    @Test
    void shouldSetAvroNamespaceFromProperties() {
        assertThat(this.graphQLToAvroConverter.getAvroNamespace()).isEqualTo("foo.bar.test.avro");
    }

    @Test
    void shouldSetAvroNamespaceInSchema() {
        final Schema parsedSchema = this.getSchema(productSchema);
        assertThat(parsedSchema.getNamespace()).isEqualTo(this.graphQLToAvroConverter.getAvroNamespace());
    }

    @Test
    void shouldConvertGraphQLSchema() {
        final Schema parsedSchema = this.getSchema(productSchema);

        assertThat(parsedSchema.getName()).isEqualTo("Product");

        assertThat(parsedSchema.getField("productId"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .extracting(Schema::getType)
            .containsExactly(Type.NULL, Type.INT);

        assertThat(parsedSchema.getField("name"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .extracting(Schema::getType)
            .contains(Type.NULL, Type.STRING);

        assertThat(parsedSchema.getField("description"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .extracting(Schema::getType)
            .contains(Type.NULL, Type.STRING);

        assertThat(parsedSchema.getField("price"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types -> assertThat(types)
                .extracting(Schema::getType)
                .containsExactly(Type.NULL, Type.RECORD)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "Price")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(2)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("value", "currency"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.NULL, Type.FLOAT, Type.NULL, Type.STRING)
            );

        assertThat(parsedSchema.getField("metadata"))
            .isNotNull()
            .extracting(Field::schema)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types -> assertThat(types)
                .extracting(Schema::getType)
                .containsExactly(Type.NULL, Type.RECORD)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "Metadata")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(2)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("created_at", "source"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.NULL, Type.INT, Type.NULL, Type.STRING)
            );
    }

    @Test
    void shouldConvertGraphQLSchemaWithLists() {
        final Schema parsedSchema = this.getSchema(contractSchema);

        assertThat(parsedSchema.getName()).isEqualTo("Contract");

        assertThat(parsedSchema.getField("_id"))
            .isNotNull()
            .extracting(field -> field.schema().getType())
            .isEqualTo(Type.STRING);

        assertThat(parsedSchema.getField("policyHolderId"))
            .isNotNull()
            .extracting(Field::schema)
            .hasFieldOrPropertyWithValue("type", Type.UNION)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "PersonGrainValue")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(3)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("_in_utc", "_v", "_c"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.STRING, Type.STRING, Type.NULL, Type.FLOAT)
            );

        assertThat(parsedSchema.getField("insuredPersonId"))
            .isNotNull()
            .extracting(Field::schema)
            .hasFieldOrPropertyWithValue("type", Type.UNION)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "PersonGrainValue")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(3)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("_in_utc", "_v", "_c"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.STRING, Type.STRING, Type.NULL, Type.FLOAT)
            );

        assertThat(parsedSchema.getField("term"))
            .isNotNull()
            .extracting(Field::schema)
            .hasFieldOrPropertyWithValue("type", Type.UNION)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "GrainValue")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(3)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("_in_utc", "_v", "_c"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.STRING, Type.STRING, Type.NULL, Type.FLOAT)
            );

        assertThat(parsedSchema.getField("value"))
            .isNotNull()
            .extracting(Field::schema)
            .hasFieldOrPropertyWithValue("type", Type.UNION)
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.ARRAY)
            )
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("type", Type.ARRAY)
            .extracting(Schema::getElementType)
            .satisfies(schema -> assertThat(schema.getType()).isEqualTo(Type.UNION))
            .extracting(Schema::getTypes, InstanceOfAssertFactories.list(Schema.class))
            .satisfies(types ->
                assertThat(types)
                    .extracting(Schema::getType)
                    .containsExactly(Type.NULL, Type.RECORD))
            .last(InstanceOfAssertFactories.type(Schema.class))
            .hasFieldOrPropertyWithValue("name", "GrainValue")
            .hasFieldOrPropertyWithValue("type", Type.RECORD)
            .extracting(Schema::getFields, InstanceOfAssertFactories.list(Field.class))
            .hasSize(3)
            .satisfies(fields -> assertThat(fields).extracting(Field::name).containsExactly("_in_utc", "_v", "_c"))
            .satisfies(fields -> assertThat(fields)
                .flatExtracting(field -> unwrapSchemaType(field.schema()))
                .containsExactly(Type.STRING, Type.STRING, Type.NULL, Type.FLOAT)
            );
    }

    @Test
    void shouldConvertAllScalars() {
        final Schema parsedSchema = this.getSchema(scalarSchema);
        assertThat(parsedSchema.getName()).isEqualTo("Scalars");

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

    private Schema getSchema(final String graphQLSchema) {
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
