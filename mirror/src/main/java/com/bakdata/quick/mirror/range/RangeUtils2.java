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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class RangeUtils2 <K,V>{

    @Nullable
    private final K key;
    @Nullable
    private final V value;
    private final String rangeField;
    private BaseRangeIndex<K, V> baseRangeIndex;

    public RangeUtils2(@Nullable final K key, @Nullable final V value, final String rangeField) {
        this.key = key;
        this.value = value;
        this.rangeField = rangeField;
    }

    public String createRangeIndex(){
        return "this.baseRangeIndex.padZeros(this.key, this.value)";
    }
}
