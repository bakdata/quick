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

import com.bakdata.quick.common.condition.ProtobufSchemaFormatCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

/**
 * Configures deserialization of Protobuf objects with Jackson.
 */
@Singleton
@Requires(condition = ProtobufSchemaFormatCondition.class)
public class ProtobufJacksonConfiguration implements ObjectMapperConfiguration {
    @Override
    public void configureObjectMapper(final ObjectMapper objectMapper) {
        final SimpleModule protoModule = new SimpleModule();
        protoModule.addSerializer(Message.class, new ProtobufMessageSerializer());
        protoModule.addSerializer(DynamicMessage.class, new ProtobufMessageSerializer());
        objectMapper.registerModule(protoModule);
    }

}
