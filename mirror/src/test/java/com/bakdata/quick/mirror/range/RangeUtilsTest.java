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

import org.junit.jupiter.api.Test;

class RangeUtilsTest {

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

    @Test
    void shouldPadZerosToInteger() {
        assertThat(RangeUtils.padZeros(ONE_DIGIT_INT_NUMBER)).isEqualTo("0000000001");
        assertThat(RangeUtils.padZeros(INT_ZERO)).isEqualTo("0000000000");
        assertThat(RangeUtils.padZeros(TWO_DIGIT_INT_NUMBER)).isEqualTo("0000000012");
        assertThat(RangeUtils.padZeros(THREE_DIGIT_INT_NUMBER)).isEqualTo("0000000123");
        assertThat(RangeUtils.padZeros(FOUR_DIGIT_INT_NUMBER)).isEqualTo("0000001234");
        assertThat(RangeUtils.padZeros(TEN_DIGIT_INT_NUMBER)).isEqualTo("1000000000");
        assertThat(RangeUtils.padZeros(TEN_DIGIT_MINUS_INT_NUMBER)).isEqualTo("-1000000000");
    }

    @Test
    void shouldPadZerosToLong() {
        assertThat(RangeUtils.padZeros(LONG_ZERO)).isEqualTo("0000000000000000000");
        assertThat(RangeUtils.padZeros(ONE_DIGIT_LONG_NUMBER)).isEqualTo("0000000000000000001");
        assertThat(RangeUtils.padZeros(TWO_DIGIT_LONG_NUMBER)).isEqualTo("0000000000000000012");
        assertThat(RangeUtils.padZeros(THREE_DIGIT_LONG_NUMBER)).isEqualTo("0000000000000000123");
        assertThat(RangeUtils.padZeros(FOUR_DIGIT_LONG_NUMBER)).isEqualTo("0000000000000001234");
        assertThat(RangeUtils.padZeros(NINETEEN_DIGIT_LONG_NUMBER)).isEqualTo("1000000000000000000");
        assertThat(RangeUtils.padZeros(NINETEEN_DIGIT_MINUS_LONG_NUMBER)).isEqualTo("-1000000000000000000");
    }
}
