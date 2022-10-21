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

package com.bakdata.quick.mirror.range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.bakdata.quick.mirror.range.indexer.DefaultRangeIndexer;
import com.bakdata.quick.mirror.range.indexer.RangeIndexer;
import com.bakdata.quick.mirror.range.indexer.StringValueRangeIndexer;
import com.bakdata.quick.testutil.AvroRangeQueryTest;
import com.bakdata.quick.testutil.ProtoRangeQueryTest;
import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import java.util.stream.Stream;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultRangeIndexerTest {

    private static final int INT_ZERO = 0;
    private static final int ONE_DIGIT_INT_NUMBER = 1;
    private static final int TWO_DIGIT_INT_NUMBER = 12;
    private static final int THREE_DIGIT_INT_NUMBER = 123;
    private static final int FOUR_DIGIT_INT_NUMBER = 1234;
    private static final int TEN_DIGIT_INT_NUMBER = 1000000000;
    private static final int TEN_DIGIT_MINUS_INT_NUMBER = -1000000000;
    private static final long LONG_ZERO = 0L;
    private static final long ONE_DIGIT_LONG_NUMBER = 1L;
    private static final long TWO_DIGIT_LONG_NUMBER = 12L;
    private static final long THREE_DIGIT_LONG_NUMBER = 123L;
    private static final long FOUR_DIGIT_LONG_NUMBER = 1234L;
    private static final long NINETEEN_DIGIT_LONG_NUMBER = 1000000000000000000L;
    private static final long NINETEEN_DIGIT_MINUS_LONG_NUMBER = -1000000000000000000L;
    public static final String RANGE_FIELD = "timestamp";

    @ParameterizedTest
    @MethodSource("integerKeyAvroValueAndRangeIndexProvider")
    void shouldCreateRangeIndexOnTimestampForIntegerKeyAndAvroValue(final int key, final GenericRecord avroRecord,
        final String rangeIndex) {
        final RangeIndexer<Integer, GenericRecord> defaultRangeIndexer =
            DefaultRangeIndexer.create(
                new AvroSchema(avroRecord.getSchema()), RANGE_FIELD);

        assertThat(defaultRangeIndexer.createIndex(key, avroRecord)).isEqualTo(rangeIndex);
    }

    @ParameterizedTest
    @MethodSource("longKeyProtobufValueAndRangeIndexProvider")
    void shouldCreateRangeIndexOnTimestampForLongKeyAndProtobufValue(final long key, final Message protoMessage,
        final String rangeIndex) {
        final RangeIndexer<Long, Message> defaultRangeIndexer =
            DefaultRangeIndexer.create(
                new ProtobufSchema(protoMessage.getDescriptorForType()), RANGE_FIELD);
        assertThat(defaultRangeIndexer.createIndex(key, protoMessage)).isEqualTo(rangeIndex);
    }

    @Test
    void shouldCreateRangeIndexOnKeyAndStringAndExclusive() {
        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        final RangeIndexer<Integer, String> stringValueRangeIndexer =
            StringValueRangeIndexer.create(
                new AvroSchema(avroRecord.getSchema()), RANGE_FIELD);

        assertThat(stringValueRangeIndexer.createIndex(1, "2")).isEqualTo("1_0000000000000000001");
    }

    @Test
    void shouldCreateRangeIndexOnNullableFieldWithKeyAndStringAndIsExclusive() {
        final AvroRangeQueryTest avroRecord =
            AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).setAge(45).build();
        final RangeIndexer<Integer, String> stringValueRangeIndexer =
            StringValueRangeIndexer.create(
                new AvroSchema(avroRecord.getSchema()), "age");

        assertThat(stringValueRangeIndexer.createIndex(1, "45")).isEqualTo("1_0000000044");
    }

    static Stream<Arguments> integerKeyAvroValueAndRangeIndexProvider() {
        final AvroRangeQueryTest avroRecord = AvroRangeQueryTest.newBuilder().setUserId(1).setTimestamp(1L).build();
        return Stream.of(
            arguments(Integer.MIN_VALUE, avroRecord, String.format("%s_0000000000000000001", Integer.MIN_VALUE)),
            arguments(INT_ZERO, avroRecord, "0_0000000000000000001"),
            arguments(ONE_DIGIT_INT_NUMBER, avroRecord, "1_0000000000000000001"),
            arguments(TWO_DIGIT_INT_NUMBER, avroRecord, "12_0000000000000000001"),
            arguments(THREE_DIGIT_INT_NUMBER, avroRecord, "123_0000000000000000001"),
            arguments(FOUR_DIGIT_INT_NUMBER, avroRecord, "1234_0000000000000000001"),
            arguments(TEN_DIGIT_INT_NUMBER, avroRecord, "1000000000_0000000000000000001"),
            arguments(TEN_DIGIT_MINUS_INT_NUMBER, avroRecord, "-1000000000_0000000000000000001"),
            arguments(Integer.MAX_VALUE, avroRecord, String.format("%s_0000000000000000001", Integer.MAX_VALUE))
        );
    }

    static Stream<Arguments> longKeyProtobufValueAndRangeIndexProvider() {
        final ProtoRangeQueryTest protoMessage = ProtoRangeQueryTest.newBuilder().setUserId(1L).setTimestamp(1).build();
        return Stream.of(
            arguments(Long.MIN_VALUE, protoMessage, String.format("%s_0000000001", Long.MIN_VALUE)),
            arguments(LONG_ZERO, protoMessage, "0_0000000001"),
            arguments(ONE_DIGIT_LONG_NUMBER, protoMessage, "1_0000000001"),
            arguments(TWO_DIGIT_LONG_NUMBER, protoMessage, "12_0000000001"),
            arguments(THREE_DIGIT_LONG_NUMBER, protoMessage, "123_0000000001"),
            arguments(FOUR_DIGIT_LONG_NUMBER, protoMessage, "1234_0000000001"),
            arguments(NINETEEN_DIGIT_LONG_NUMBER, protoMessage, "1000000000000000000_0000000001"),
            arguments(NINETEEN_DIGIT_MINUS_LONG_NUMBER, protoMessage, "-1000000000000000000_0000000001"),
            arguments(Long.MAX_VALUE, protoMessage, String.format("%s_0000000001", Long.MAX_VALUE))
        );
    }
}
