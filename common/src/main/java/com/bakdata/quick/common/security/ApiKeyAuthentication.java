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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.config.TokenConfiguration;
import java.util.Map;

/**
 * Basic authentication representation for auth based on api keys.
 *
 * <p>
 * The api keys do not contain any information about username or roles. Therefore, we return a default name and no
 * roles.
 */
public class ApiKeyAuthentication implements Authentication {
    private static final String DEFAULT_NAME = "API-KEY";

    @NonNull
    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(TokenConfiguration.DEFAULT_NAME_KEY, DEFAULT_NAME);
    }

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }
}
