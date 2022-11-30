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
import com.bakdata.quick.mirror.range.extractor.value.FieldValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.GenericRecordValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
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
        final FieldValueExtractor<GenericRecord> avroExtractor = new GenericRecordValueExtractor<>();
        assertThat(avroExtractor.extract(avroRecord, TIMESTAMP_FIELD, Long.class)).isEqualTo(3L);
    }

    @Test
    void shouldExtractValueFromProtobufSchema() {
        final ProtoRangeQueryTest protoMessage = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(5).build();
        final FieldValueExtractor<Message> protoExtractor = new MessageValueExtractor<>();
        assertThat(protoExtractor.extract(protoMessage, TIMESTAMP_FIELD, Integer.class)).isEqualTo(5);
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExistInAvroSchema() {
        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(3L).build();
        final FieldValueExtractor<GenericRecord> avroExtractor = new GenericRecordValueExtractor<>();
        final String errorMessage = String.format("Could not find field with name %s", NON_EXISTING_FIELD);

        assertThatThrownBy(() -> avroExtractor.extract(avroRecord, NON_EXISTING_FIELD, Long.class)).isInstanceOf(
            MirrorTopologyException.class).hasMessageContaining(errorMessage);
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExistInProtobufSchema() {
        final ProtoRangeQueryTest protoMessage = ProtoRangeQueryTest.newBuilder().setUserId(1).setTimestamp(5).build();
        final FieldValueExtractor<Message> protoExtractor = new MessageValueExtractor<>();
        final String errorMessage = String.format("Could not find field with name %s", NON_EXISTING_FIELD);

        assertThatThrownBy(() -> protoExtractor.extract(protoMessage, NON_EXISTING_FIELD, Integer.class))
            .isInstanceOf(MirrorTopologyException.class).hasMessageContaining(errorMessage);
    }
}
