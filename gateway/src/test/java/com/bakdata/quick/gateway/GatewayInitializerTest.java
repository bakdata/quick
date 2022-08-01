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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Note: We use {@code TempDir} to mock that real schemas are an external resource.
 * In contrast to files in the test resource directory, they cannot be accessed by the class loader.
 */
@MicronautTest
@Property(name = "quick.kafka.bootstrap-server", value = "dummy:1234")
@Property(name = "quick.kafka.schema-registry-url", value = "http://dummy")
@Property(name = "quick.definition.path", value = "/definition/schema.graphql")
class GatewayInitializerTest {

    private static final Path workingDirectory = Path.of("src", "test", "resources", "initializer");
    @Inject
    private QuickGraphQLContext context;
    @Inject
    private ApplicationContext applicationContext;

    @Test
    void shouldReadDefinitionOnStartUp(@TempDir final Path tempDir) throws IOException {
        final String fileName = "schema.graphql";
        final Path sourceFile = workingDirectory.resolve(fileName);
        final Path tempFile = tempDir.resolve(fileName);
        Files.copy(sourceFile, tempFile);
        final String expected = Files.readString(sourceFile);

        final GatewayConfig gatewayConfig = new GatewayConfig(tempFile.toString());
        final GatewayInitializer gatewayInitializer = new GatewayInitializer(this.context, gatewayConfig);

        gatewayInitializer.onApplicationEvent(new StartupEvent(this.applicationContext));
        verify(this.context).updateFromSchemaString(expected);
    }

    @Test
    void shouldNotUpdateWhenDefinitionIsEmpty(@TempDir final Path tempDir) throws IOException {
        // create empty file
        final Path tempFile = Files.createFile(tempDir.resolve("empty-schema.graphql"));

        final GatewayConfig gatewayConfig = new GatewayConfig(tempFile.toString());
        final GatewayInitializer gatewayInitializer = new GatewayInitializer(this.context, gatewayConfig);
        gatewayInitializer.onApplicationEvent(new StartupEvent(this.applicationContext));
        verify(this.context, never()).updateFromSchemaString(anyString());
    }

    @Test
    void shouldFailSilentlyIfNoFileExists() {
        final GatewayConfig gatewayConfig = new GatewayConfig("non-existing");
        final GatewayInitializer gatewayInitializer = new GatewayInitializer(this.context, gatewayConfig);
        assertThatNoException().isThrownBy(() ->
            gatewayInitializer.onApplicationEvent(new StartupEvent(this.applicationContext)));
    }

    @MockBean(QuickGraphQLContext.class)
    QuickGraphQLContext gatewayUpdater() {
        return mock(QuickGraphQLContext.class);
    }
}
