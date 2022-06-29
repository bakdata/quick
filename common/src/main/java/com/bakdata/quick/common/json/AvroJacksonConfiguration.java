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

import com.bakdata.quick.common.condition.AvroSchemaFormatCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Requires;
import javax.inject.Singleton;
import org.apache.avro.generic.GenericRecord;


/**
 * Configures deserialization of Avro objects with Jackson.
 */
@Singleton
@Requires(condition = AvroSchemaFormatCondition.class)
public class AvroJacksonConfiguration implements ObjectMapperConfiguration {

    @Override
    public void configureObjectMapper(final ObjectMapper objectMapper) {
        // necessary to correctly (de)serialize timestamp in avro classes
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // add avro module
        final SimpleModule avroModule = new SimpleModule();
        avroModule.addSerializer(GenericRecord.class, new AvroRecordSerializer());
        objectMapper.registerModule(avroModule);
    }
}
