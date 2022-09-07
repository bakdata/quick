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

public class LongPadder implements ZeroPadder<Long> {
    private static final int MAX_LONG_LENGTH = 19;

    @Override
    public String padZero(final Long number) {
        final DecimalFormat decimalFormat = new DecimalFormat("0".repeat(MAX_LONG_LENGTH));
        return decimalFormat.format(number);
    }

    @Override
    public Class<Long> getPadderClass() {
        return Long.class;
    }
}
