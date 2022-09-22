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

package com.bakdata.quick.mirror.range.padder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ZeroPadderTest {
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

    @ParameterizedTest
    @MethodSource("integerNumberProvider")
    void shouldPadZerosToIntegers(final int number, final String expected) {
        final ZeroPadder<Integer> integerZeroPadder = new IntPadder();
        assertThat(integerZeroPadder.getPadderClass()).isEqualTo(Integer.class);
        assertThat(integerZeroPadder.padZero(number)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("longNumberProvider")
    void shouldPadZerosToLongs(final long number, final String expected) {
        final ZeroPadder<Long> longZeroPadder = new LongPadder();
        assertThat(longZeroPadder.getPadderClass()).isEqualTo(Long.class);
        assertThat(longZeroPadder.padZero(number)).isEqualTo(expected);
    }


    static Stream<Arguments> integerNumberProvider() {
        return Stream.of(
            arguments(Integer.MIN_VALUE, String.valueOf(Integer.MIN_VALUE)),
            arguments(INT_ZERO, "0000000000"),
            arguments(ONE_DIGIT_INT_NUMBER, "0000000001"),
            arguments(TWO_DIGIT_INT_NUMBER, "0000000012"),
            arguments(THREE_DIGIT_INT_NUMBER, "0000000123"),
            arguments(FOUR_DIGIT_INT_NUMBER, "0000001234"),
            arguments(TEN_DIGIT_INT_NUMBER, "1000000000"),
            arguments(TEN_DIGIT_MINUS_INT_NUMBER, "-1000000000"),
            arguments(Integer.MAX_VALUE, String.valueOf(Integer.MAX_VALUE))
        );
    }

    static Stream<Arguments> longNumberProvider() {
        return Stream.of(
            arguments(Long.MIN_VALUE, String.valueOf(Long.MIN_VALUE)),
            arguments(LONG_ZERO, "0000000000000000000"),
            arguments(ONE_DIGIT_LONG_NUMBER, "0000000000000000001"),
            arguments(TWO_DIGIT_LONG_NUMBER, "0000000000000000012"),
            arguments(THREE_DIGIT_LONG_NUMBER, "0000000000000000123"),
            arguments(FOUR_DIGIT_LONG_NUMBER, "0000000000000001234"),
            arguments(NINETEEN_DIGIT_LONG_NUMBER, "1000000000000000000"),
            arguments(NINETEEN_DIGIT_MINUS_LONG_NUMBER, "-1000000000000000000"),
            arguments(Long.MAX_VALUE, String.valueOf(Long.MAX_VALUE))
        );
    }
}
