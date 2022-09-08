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

package com.bakdata.quick.mirror.range.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bakdata.quick.common.exception.MirrorTopologyException;
import com.bakdata.quick.testutil.AvroRangeQueryTest;
import com.bakdata.quick.testutil.ProtoRangeQueryTest;
import com.google.protobuf.Message;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

class FieldValueExtractorTest {
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String NON_EXISTING_FIELD = "non-existing-filed";

    @Test
    void shouldExtractValueFromAvroSchema() {
        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final RangeFieldValueExtractor<GenericRecord, Long> avroExtractor = new AvroValueExtractor<>(Long.class);
        assertThat(avroExtractor.extractValue(avroRecord, TIMESTAMP_FIELD)).isEqualTo(3L);
    }

    @Test
    void shouldExtractValueFromProtobufSchema() {
        final ProtoRangeQueryTest protoMessage = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(5).build();
        final RangeFieldValueExtractor<Message, Integer> protoExtractor = new ProtoValueExtractor<>(Integer.class);
        assertThat(protoExtractor.extractValue(protoMessage, TIMESTAMP_FIELD)).isEqualTo(5);
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExistInAvroSchema() {
        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final RangeFieldValueExtractor<GenericRecord, Long> avroExtractor = new AvroValueExtractor<>(Long.class);
        final String errorMessage = String.format("Could not find range field with name %s", NON_EXISTING_FIELD);

        assertThatThrownBy(() -> avroExtractor.extractValue(avroRecord, NON_EXISTING_FIELD)).isInstanceOf(
            MirrorTopologyException.class).hasMessageContaining(errorMessage);
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExistInProtobufSchema() {
        final ProtoRangeQueryTest protoMessage = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(5).build();
        final RangeFieldValueExtractor<Message, Integer> protoExtractor = new ProtoValueExtractor<>(Integer.class);
        final String errorMessage = String.format("Could not find range field with name %s", NON_EXISTING_FIELD);

        assertThatThrownBy(() -> protoExtractor.extractValue(protoMessage, NON_EXISTING_FIELD))
            .isInstanceOf(MirrorTopologyException.class).hasMessageContaining(errorMessage);
    }
}
