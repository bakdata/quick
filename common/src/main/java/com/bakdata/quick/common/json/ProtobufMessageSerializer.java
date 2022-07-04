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
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

/**
 * JSON serializer for Protobuf records.
 *
 * <p>
 * This internally delegates deserialization to Protobuf's {@link JsonFormat.Printer}.
 */
final class ProtobufMessageSerializer extends JsonSerializer<Message> {
    private final JsonFormat.Printer jsonProtoPrinter;

    ProtobufMessageSerializer() {
        this.jsonProtoPrinter = JsonFormat.printer()
            .includingDefaultValueFields();
    }

    @Override
    public void serialize(final Message message, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        final String json = this.jsonProtoPrinter.print(message);
        jsonGenerator.writeRawValue(json);
    }
}
