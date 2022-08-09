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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * Factory customizing {@link MeterFilter} for micrometer support.
 */
@RequiresMetrics
@Factory
public class MetricsFactory {
    public static final String DEFAULT_APP_NAME = "quick-app";
    public static final String DASHBOARD_TAG_NAME = "application";

    private final String applicationName;

    public MetricsFactory(final ApplicationConfiguration appConfig) {
        this.applicationName = appConfig.getName().orElse(DEFAULT_APP_NAME);
    }

    /**
     * Add global tag {@link MetricsFactory#DASHBOARD_TAG_NAME} to all metrics.
     *
     * <p>
     * This is can be used for the official micrometer dashboard, see:
     * <a href="https://grafana.com/grafana/dashboards/4701">Dashboard</a>
     *
     * @return meter filter
     */
    @Bean
    @Singleton
    public MeterFilter addCommonTagFilter() {
        return MeterFilter.commonTags(List.of(Tag.of(DASHBOARD_TAG_NAME, this.applicationName)));
    }

}
