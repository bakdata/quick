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

/**
 * Pads zeros to the left number.
 *
 * @param <T> Type of the number
 */
public interface ZeroPadder<T> {

    /**
     * Converts the value T to a numerical string with zeros padded to the left to keep the lexicographical order.
     */
    String padZero(final T number);

    /**
     * Return the class type of T.
     */
    Class<T> getPadderClass();

    /**
     * The implementation of this method should return the end of the range. The end of the range could be exclusive or
     * inclusive.
     */
    T getEndOfRange(final String stringValue);
}
