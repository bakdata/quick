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

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

/**
 * A resolver for Protobuf messages.
 */
public class ProtobufResolver implements TypeResolver<Message> {
    private final JsonFormat.Parser parser;
    private final Descriptors.Descriptor descriptor;

    public ProtobufResolver(final Descriptors.Descriptor descriptor) {
        this.parser = JsonFormat.parser();
        this.descriptor = descriptor;
    }

    @Override
    public Message fromString(final String value) {
        try {
            final DynamicMessage.Builder builder = DynamicMessage.newBuilder(this.descriptor);
            this.parser.merge(value, builder);
            return builder.build();
        } catch (final InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
