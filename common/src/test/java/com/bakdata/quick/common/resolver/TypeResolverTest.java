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

package com.bakdata.quick.common.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.avro.ChartRecord;
import com.bakdata.quick.common.api.model.AvroQuickTopicType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

class TypeResolverTest {
    private static final Path workingDirectory = Path.of("src", "test", "resources");
    private static final long EXPECTED_FIELD_ID = 5L;
    private static final long EXPECTED_COUNT_PLAYS = 10L;
    private static final String JSON_RECORD = "{\"fieldId\":5,\"countPlays\":10}";
    private static final Map<String, Long> MAP_RECORD = Map.of("fieldId", 5L, "countPlays", 10L);
    private final ChartRecord chartRecord = ChartRecord.newBuilder().setFieldId(5L).setCountPlays(10L).build();

    @Test
    void testValueFromString() {
        final GenericAvroResolver resolver = new GenericAvroResolver();
        resolver.configure(new AvroSchema(ChartRecord.getClassSchema()));
        final GenericRecord genericRecord = resolver.fromString(JSON_RECORD);
        assertThat(genericRecord.getSchema()).isEqualTo(this.chartRecord.getSchema());
        assertThat(genericRecord.get("fieldId")).isEqualTo(EXPECTED_FIELD_ID);
        assertThat(genericRecord.get("countPlays")).isEqualTo(EXPECTED_COUNT_PLAYS);
    }

    @Test
    void testRecordToString() {
        final TypeResolver<GenericRecord> resolver = new GenericAvroResolver();
        resolver.configure(new AvroSchema(ChartRecord.getClassSchema()));
        final String resolvedRecord = resolver.toString(this.chartRecord);
        assertThat(resolvedRecord).isEqualTo(JSON_RECORD);
    }
    
    @Test
    void testValueFromObject() {
        final GenericAvroResolver resolver = new GenericAvroResolver();
        resolver.configure(new AvroSchema(ChartRecord.getClassSchema()));
        final GenericRecord genericRecord = resolver.fromObject(MAP_RECORD);
        assertThat(genericRecord.getSchema()).isEqualTo(this.chartRecord.getSchema());
        assertThat(genericRecord.get("fieldId")).isEqualTo(EXPECTED_FIELD_ID);
        assertThat(genericRecord.get("countPlays")).isEqualTo(EXPECTED_COUNT_PLAYS);
    }

    @Test
    void checkNullable() throws IOException {
        final String schema = Files.readString(workingDirectory.resolve("product-schema.avsc"));
        final GenericAvroResolver resolver = new GenericAvroResolver();
        resolver.configure(new AvroSchema(schema));

        final ObjectMapper objectMapper = new ObjectMapper();
        final String data = Files.readString(workingDirectory.resolve("product.json"));
        final GenericRecord record = resolver.fromString(data);
        assertThat(objectMapper.readTree(record.toString())).isEqualTo(objectMapper.readTree(data));
    }

    @Test
    void testIntegerFromObject() {
        final IntegerResolver resolver = new IntegerResolver();

        assertThat(resolver.fromObject(5)).isEqualTo(5);
        assertThat(resolver.fromObject(Integer.valueOf("5"))).isEqualTo(resolver.fromObject(5));
    }

    @Test
    void testLongFromObject() {
        final LongResolver resolver = new LongResolver();

        assertThat(resolver.fromObject(5L)).isEqualTo(5L);
        assertThat(resolver.fromObject(Long.valueOf("5"))).isEqualTo(5L);
    }

    @Test
    void testDoubleFromObject() {
        final DoubleResolver resolver = new DoubleResolver();

        assertThat(resolver.fromObject(5.)).isEqualTo(5.);
        assertThat(resolver.fromObject(Double.valueOf("5."))).isEqualTo(5.);
    }

    @Test
    void testStringFromObject() {
        final StringResolver resolver = new StringResolver();

        assertThat(resolver.fromObject("5")).isEqualTo("5");
        assertThat(resolver.fromObject("5")).isEqualTo("5");
    }
}
