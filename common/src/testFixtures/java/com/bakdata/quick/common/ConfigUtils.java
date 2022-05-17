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
        try (final ApplicationContext context = ApplicationContext.run(propertySource, propertySource.getName())) {
            return context.getBean(configClass);
        }
    }
}
