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

package com.bakdata.quick.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.inject.Singleton;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;


/**
 * Configures deserialization of avro object with Jackson.
 *
 * <p>
 * {@link SpecificRecord} instances contain fields that should not be deserialized. This class hooks into micronaut's
 * bean lifecycle, and configures the {@link ObjectMapper} so that it ignores those fields.
 */
@Singleton
public class AvroJacksonConfiguration implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(final BeanCreatedEvent<ObjectMapper> event) {
        final ObjectMapper objectMapper = event.getBean();
        // necessary to correctly (de)serialize timestamp in avro classes
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        final SimpleModule avroModule = new SimpleModule();
        avroModule.addSerializer(GenericRecord.class, new AvroRecordSerializer());
        objectMapper.registerModule(avroModule);
        return objectMapper;
    }

    /**
     * JSON serializer for Avro records.
     *
     * <p>
     * Since {@code SpecificRecord} implements {@link GenericRecord}, this class is used for both types.
     */
    private static final class AvroRecordSerializer extends JsonSerializer<GenericRecord> {
        private final JsonAvroConverter jsonAvroConverter;

        private AvroRecordSerializer() {
            this.jsonAvroConverter = new JsonAvroConverter();
        }

        @Override
        public void serialize(final GenericRecord genericRecord, final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {
            final String json = new String(this.jsonAvroConverter.convertToJson(genericRecord), StandardCharsets.UTF_8);
            jsonGenerator.writeRawValue(json);
        }
    }
}
