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

package com.bakdata.quick.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * Listener for Micronaut's bean event that configures {@link ObjectMapper} with {@link ObjectMapperConfiguration}.
 */
@Singleton
public class ObjectMapperListener implements BeanCreatedEventListener<ObjectMapper> {
    private final List<? extends ObjectMapperConfiguration> configurations;

    @Inject
    public ObjectMapperListener(final List<? extends ObjectMapperConfiguration> configurations) {
        this.configurations = configurations;
    }

    @Override
    public ObjectMapper onCreated(final BeanCreatedEvent<ObjectMapper> event) {
        final ObjectMapper objectMapper = event.getBean();
        this.configurations.forEach(configuration -> configuration.configureObjectMapper(objectMapper));
        return objectMapper;
    }
}
