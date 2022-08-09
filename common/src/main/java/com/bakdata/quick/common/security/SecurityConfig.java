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

package com.bakdata.quick.common.security;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import lombok.Getter;

/**
 * Configuration for Quick's security.
 */
@Singleton
@Getter
public class SecurityConfig {
    private final boolean securityEnabled;

    private final String apiKey;

    public SecurityConfig(
        @Property(name = "micronaut.security.enabled", defaultValue = "true") final boolean securityEnabled,
        @Property(name = "quick.apikey", defaultValue = "none") final String apiKey) {
        this.securityEnabled = securityEnabled;
        this.apiKey = apiKey;
    }
}
