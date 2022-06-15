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

package com.bakdata.quick.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.avro.generic.GenericRecord;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

/**
 * JSON serializer for Avro records.
 *
 * <p>
 * Since {@code SpecificRecord} implements {@link GenericRecord}, this class is used for both types.
 * A custom serializer is required, because we want to skip certain fields of Avro records.
 * This uses the implementation provided by {@link JsonAvroConverter} to handle other cases like nullable unions.
 */
final class AvroRecordSerializer extends JsonSerializer<GenericRecord> {
    private final JsonAvroConverter jsonAvroConverter;

    AvroRecordSerializer() {
        this.jsonAvroConverter = new JsonAvroConverter();
    }

    @Override
    public void serialize(final GenericRecord genericRecord, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        final String json = new String(this.jsonAvroConverter.convertToJson(genericRecord), StandardCharsets.UTF_8);
        jsonGenerator.writeRawValue(json);
    }
}
