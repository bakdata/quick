package com.bakdata.quick.common;

import io.micronaut.context.env.MapPropertySource;
import java.util.Map;

/**
 * Property source for testing manually set environment variables.
 */
public class TestEnvironmentPropertySource extends MapPropertySource {
    public static final String NAME = "test";

    public TestEnvironmentPropertySource(final Map map) {
        super(NAME, map);
    }

    @Override
    public PropertyConvention getConvention() {
        return PropertyConvention.ENVIRONMENT_VARIABLE;
    }
}
