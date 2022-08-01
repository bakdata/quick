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

package com.bakdata.quick.gateway;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for reading an existing schema.graphql from file.
 */
@Singleton
@Slf4j
public class GatewayInitializer implements ApplicationEventListener<StartupEvent> {

    private final QuickGraphQLContext context;
    private final GatewayConfig gatewayConfig;

    @Inject
    public GatewayInitializer(final QuickGraphQLContext context, final GatewayConfig gatewayConfig) {
        this.context = context;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void onApplicationEvent(final StartupEvent event) {
        final Path gatewayConfigPath = Path.of(this.gatewayConfig.getPath());

        if (!Files.exists(gatewayConfigPath)) {
            log.warn("No schema file found");
            return;
        }
        try {
            final String schema = Files.readString(gatewayConfigPath);
            if (schema.isBlank()) {
                log.info("No schema applied: File is empty");
            } else {
                log.info("Read schema from file: {}", schema);
                this.context.updateFromSchemaString(schema);
            }
        } catch (final IOException e) {
            // something went seriously wrong, shut down
            throw new UncheckedIOException("Something went wrong while reading the schema", e);
        }
    }
}
