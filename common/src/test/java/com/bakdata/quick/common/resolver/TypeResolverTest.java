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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.parsing.Parser;
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
        final GenericAvroResolver resolver = new GenericAvroResolver(ChartRecord.getClassSchema());
        final GenericRecord genericRecord = resolver.fromString(JSON_RECORD);
        assertThat(genericRecord.getSchema()).isEqualTo(this.chartRecord.getSchema());
        assertThat(genericRecord.get("fieldId")).isEqualTo(EXPECTED_FIELD_ID);
        assertThat(genericRecord.get("countPlays")).isEqualTo(EXPECTED_COUNT_PLAYS);
    }

    @Test
    void testKnownTypeFromString() {
        final KnownTypeRecord exepected = KnownTypeRecord.builder().fieldId(5L).countPlays(10L).build();
        final TypeResolver<KnownTypeRecord> typeResolver =
            new KnownTypeResolver<>(KnownTypeRecord.class, new ObjectMapper());
        final KnownTypeRecord knownTypeRecord = typeResolver.fromString(JSON_RECORD);
        assertThat(knownTypeRecord).isEqualTo(exepected);
    }


    @Test
    void checkNullable() throws IOException {
        final String schema = Files.readString(workingDirectory.resolve("product-schema.avsc"));
        final Schema parsedSchema = new Schema.Parser().parse(schema);
        final GenericAvroResolver resolver = new GenericAvroResolver(parsedSchema);

        final ObjectMapper objectMapper = new ObjectMapper();
        final String data = Files.readString(workingDirectory.resolve("product.json"));
        final GenericRecord record = resolver.fromString(data);
        assertThat(objectMapper.readTree(record.toString())).isEqualTo(objectMapper.readTree(data));
    }

    @Value
    @Builder
    static class KnownTypeRecord {
        long fieldId;
        long countPlays;
    }
}
