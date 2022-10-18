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

package com.bakdata.quick.mirror.service.context;


import static com.bakdata.quick.mirror.MirrorApplication.RANGE_STORE;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Value;

/**
 * Contains the range index properties.
 */
@Value
public class RangeIndexProperties {
    String storeName;
    @Nullable
    String rangeField;

    /**
     * Default range index properties.
     */
    public static RangeIndexProperties createDefault() {
        return new RangeIndexProperties(RANGE_STORE, null);
    }
}