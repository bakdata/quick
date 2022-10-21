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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Duration;
import lombok.Value;

/**
 * Contains the retention time index properties.
 */
@Value
public class RetentionTimeProperties {
    String storeName;
    @Nullable
    Duration retentionTime;

    /**
     * Checks if the retention time topology is enabled or not.
     */
    public boolean isEnabled() {
        return this.retentionTime != null;
    }
}
