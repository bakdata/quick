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

package com.bakdata.quick.manager.config;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Defines the image pull policies of the manager resources.
 *
 * @see ApplicationSpecificationConfig
 */
public enum ImagePullPolicy {
    IF_NOT_PRESENT("IfNotPresent"),
    ALWAYS("Always"),
    NEVER("Never");

    @Getter
    @JsonValue
    private final String policyName;

    ImagePullPolicy(final String policyName) {
        this.policyName = policyName;
    }
}
