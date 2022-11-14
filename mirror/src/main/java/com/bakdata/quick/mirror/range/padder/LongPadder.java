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
 * Pads zeros to the left of a long number.
 */
public class LongPadder implements ZeroPadder<Long> {
    private static final int MAX_LONG_LENGTH = 19;
    private final DecimalFormat decimalFormat = new DecimalFormat("0".repeat(MAX_LONG_LENGTH));
    private final EndRange endRange;

    /**
     * Default constructor.
     *
     * @param endRange determines if the value should be exclusive or not
     */
    public LongPadder(final EndRange endRange) {
        this.endRange = endRange;
    }

    /**
     * Pads the long number with zeros and converts it into a string with 19 digits.
     */
    @Override
    public String padZero(final Long number) {
        return this.decimalFormat.format(number);
    }

    @Override
    public Class<Long> getPadderClass() {
        return Long.class;
    }

    /**
     * Converts a given numeric string value to a long. If the end range is exclusive, the value is decreased.
     */
    @Override
    public Long getEndOfRange(final String stringValue) {
        final long longNumber = Long.parseLong(stringValue);
        if (this.endRange == EndRange.EXCLUSIVE) {
            return longNumber - 1L;
        }
        return longNumber;
    }
}
