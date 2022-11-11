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

package com.bakdata.quick.common.config;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A factory for registering beans manually.
 */
@Factory
public class BeanFactory {

    /**
     * UUID supplier needed for {@link com.bakdata.quick.common.api.client.RequestHeaderFilter}.
     *
     * @return a UUID supplier
     */
    @Singleton
    @Named("RequestIdSupplier")
    public Supplier<UUID> getUuidSupplier() {
        return UUID::randomUUID;
    }
}
