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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.bakdata.quick.common.api.model.AvroQuickTopicType;
import com.bakdata.quick.common.api.model.AvroTopicData;
import com.bakdata.quick.common.api.model.AvroWriteType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@MicronautTest
class AvroResolverTest {
    @Inject
    private ObjectMapper objectMapper;

    @Test
    void shouldDeserializeRecord() {
        final AvroTopicData data = AvroTopicData.newBuilder()
            .setKeyType(AvroQuickTopicType.DOUBLE)
            .setValueType(AvroQuickTopicType.INTEGER)
            .setName("test")
            .setWriteType(AvroWriteType.MUTABLE)
            .build();

        assertDoesNotThrow(() -> this.objectMapper.writeValueAsString(data));
    }


    @Test
    void shouldDeserializeListOfRecords() {
        final List<AvroTopicData> data = List.of(
            AvroTopicData.newBuilder()
                .setKeyType(AvroQuickTopicType.DOUBLE)
                .setValueType(AvroQuickTopicType.INTEGER)
                .setName("test")
                .setWriteType(AvroWriteType.MUTABLE)
                .setSchema$("kasdj")
                .build(),
            AvroTopicData.newBuilder()
                .setKeyType(AvroQuickTopicType.DOUBLE)
                .setValueType(AvroQuickTopicType.INTEGER)
                .setName("test")
                .setWriteType(AvroWriteType.MUTABLE)
                .build()
        );

        assertDoesNotThrow(() -> this.objectMapper.writeValueAsString(data));
    }
}
