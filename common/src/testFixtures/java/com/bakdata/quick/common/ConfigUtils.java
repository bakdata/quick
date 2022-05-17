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
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigUtils {

    public <T> T createWithProperties(final Map<String, Object> properties, final Class<T> configClass) {
        return createWithSource(MapPropertySource.of(TestEnvironmentPropertySource.NAME, properties), configClass);
    }

    public <T> T createWithEnvironment(final Map<String, Object> environment, final Class<T> configClass) {
        return createWithSource(new TestEnvironmentPropertySource(environment), configClass);
    }

    private <T> T createWithSource(final PropertySource propertySource, final Class<T> configClass) {
        try (final ApplicationContext context = createWithSource(propertySource)) {
            return context.getBean(configClass);
        }
    }

    public ApplicationContext createWithProperties(final Map<String, Object> properties) {
        return createWithSource(MapPropertySource.of(TestEnvironmentPropertySource.NAME, properties));
    }

    public ApplicationContext createWithEnvironment(final Map<String, Object> environment) {
        return createWithSource(new TestEnvironmentPropertySource(environment));
    }

    private ApplicationContext createWithSource(final PropertySource propertySource) {
        return ApplicationContext.run(propertySource, propertySource.getName());
    }
}
