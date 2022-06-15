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

package com.bakdata.quick.common;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.context.env.PropertySource;
import java.util.Map;

/**
 * Utils for testing configuration within micronaut apps.
 */
public final class ConfigUtils {

    private ConfigUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static <T> T createWithProperties(final Map<String, Object> properties, final Class<T> configClass) {
        return createWithSource(MapPropertySource.of(TestEnvironmentPropertySource.NAME, properties), configClass);
    }

    public static ApplicationContext createWithProperties(final Map<String, Object> properties) {
        return createWithSource(MapPropertySource.of(TestEnvironmentPropertySource.NAME, properties));
    }

    public static <T> T createWithEnvironment(final Map<String, Object> environment, final Class<T> configClass) {
        return createWithSource(new TestEnvironmentPropertySource(environment), configClass);
    }

    public static ApplicationContext createWithEnvironment(final Map<String, Object> environment) {
        return createWithSource(new TestEnvironmentPropertySource(environment));
    }

    private static <T> T createWithSource(final PropertySource propertySource, final Class<T> configClass) {
        try (final ApplicationContext context = createWithSource(propertySource)) {
            return context.getBean(configClass);
        }
    }

    private static ApplicationContext createWithSource(final PropertySource propertySource) {
        return ApplicationContext.run(propertySource, propertySource.getName());
    }
}
