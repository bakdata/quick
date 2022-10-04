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

    @Override
    public String padZero(final Integer number) {
        return this.decimalFormat.format(number);
    }

    @Override
    public Class<Integer> getPadderClass() {
        return Integer.class;
    }
}
