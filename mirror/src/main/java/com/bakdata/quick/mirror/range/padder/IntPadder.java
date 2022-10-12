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

import java.text.DecimalFormat;

/**
 * Implements the {@link ZeroPadder} interface for integer.
 */
public class IntPadder implements ZeroPadder<Integer> {
    private static final int MAX_INTEGER_LENGTH = 10;
    private final DecimalFormat decimalFormat = new DecimalFormat("0".repeat(MAX_INTEGER_LENGTH));
    private final EndRange endRange;

    /**
     * Default constructor.
     *
     * @param endRange determines if the value should be exclusive or not
     */
    public IntPadder(final EndRange endRange) {
        this.endRange = endRange;
    }

    /**
     * Pads the int number with zeros and converts it into a string with 10 digits.
     */
    @Override
    public String padZero(final Integer number) {
        return this.decimalFormat.format(number);
    }

    @Override
    public Class<Integer> getPadderClass() {
        return Integer.class;
    }

    /**
     * Converts a given numeric string value to a integer. If the end range is exclusive the value is decreased.
     */
    @Override
    public Integer getEndOfRange(final String stringValue) {
        final int intNumber = Integer.parseInt(stringValue);
        if (this.endRange == EndRange.EXCLUSIVE) {
            return intNumber - 1;
        }
        return intNumber;
    }
}
