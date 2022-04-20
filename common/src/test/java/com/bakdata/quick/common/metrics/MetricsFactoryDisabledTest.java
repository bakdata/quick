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

package com.bakdata.quick.common.metrics;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = MICRONAUT_METRICS_ENABLED, value = StringUtils.FALSE)
class MetricsFactoryDisabledTest {
    @Inject
    ApplicationContext context;

    @Test
    void isDisabledByDefault() {
        assertThat(this.context.findBean(MetricsFactory.class)).isNotPresent();
    }
}
