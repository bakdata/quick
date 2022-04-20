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

package com.bakdata.quick.gateway.directives.rest;


import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethodType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class RestDirectiveTest {
    private static final Path workingDir = Path.of("", "src/test/resources/schema/directives/rest/directive");

    private SchemaParser schemaParser;
    private SchemaGenerator schemaGenerator;
    private List<RestDirective> directives;
    private RuntimeWiring wiring;

    @BeforeEach
    void setUp() {
        this.schemaParser = new SchemaParser();
        this.schemaGenerator = new SchemaGenerator();
        this.directives = new ArrayList<>();
        this.wiring = RuntimeWiring.newRuntimeWiring()
            .directive("rest", new TestWiring(this.directives))
            .build();
    }

    @Test
    void shouldParseNonOptionalArguments(final TestInfo testInfo) throws IOException {
        this.parseSchema(testInfo);

        assertThat(this.directives)
            .hasSize(1)
            .first()
            .satisfies(directive -> {
                assertThat(directive.getUrl()).isEqualTo("http://localhost:8081");
                assertThat(directive.getPathParameters()).isEmpty();
                assertThat(directive.getQueryParameters()).isEmpty();
                // assert default
                assertThat(directive.getRestDirectiveMethod()).isEqualTo(RestDirectiveMethod.GET);
            });
    }

    @Test
    void shouldParseOptionalArguments(final TestInfo testInfo) throws IOException {
        this.parseSchema(testInfo);

        assertThat(this.directives)
            .hasSize(1)
            .first()
            .satisfies(directive -> {
                assertThat(directive.getUrl()).isEqualTo("http://localhost:8081");
                assertThat(directive.getPathParameters()).hasSize(1).first().isEqualTo("userId");
                assertThat(directive.getQueryParameters()).hasSize(1).first().isEqualTo("limit");
            });
    }

    @Test
    void shouldParseMultipleDirectives(final TestInfo testInfo) throws IOException {
        this.parseSchema(testInfo);

        assertThat(this.directives)
            .hasSize(2)
            .anySatisfy(directive -> assertThat(directive.getUrl()).isEqualTo("http://localhost:8081"))
            .anySatisfy(directive -> assertThat(directive.getUrl()).isEqualTo("http://localhost:8082"));
    }

    @Test
    void shouldParseHttpMethod(final TestInfo testInfo) throws IOException {
        this.parseSchema(testInfo);

        assertThat(this.directives)
            .hasSize(1)
            .first()
            .satisfies(directive -> {
                assertThat(directive.getUrl()).isEqualTo("http://localhost:8081");
                assertThat(directive.getPathParameters()).hasSize(1).first().isEqualTo("userId");
                assertThat(directive.getQueryParameters()).hasSize(1).first().isEqualTo("limit");
                assertThat(directive.getRestDirectiveMethod()).isEqualTo(RestDirectiveMethod.POST);
            });
    }

    @Test
    void shouldParseBodyParameter(final TestInfo testInfo) throws IOException {
        this.parseSchema(testInfo);

        assertThat(this.directives)
            .hasSize(1)
            .first()
            .satisfies(directive -> {
                assertThat(directive.getUrl()).isEqualTo("http://localhost:8081");
                assertThat(directive.getPathParameters()).isEmpty();
                assertThat(directive.getQueryParameters()).isEmpty();
                assertThat(directive.getBodyParameter()).isEqualTo("inputs");
                assertThat(directive.getRestDirectiveMethod()).isEqualTo(RestDirectiveMethod.POST);
            });
    }

    private void parseSchema(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final String schemaString = Files.readString(workingDir.resolve(name + ".graphql"));
        final TypeDefinitionRegistry registry = this.schemaParser.parse(schemaString);
        registry.add(RestDirective.DEFINITION);
        registry.add(RestDirectiveMethodType.DEFINITION);
        this.schemaGenerator.makeExecutableSchema(registry, this.wiring);
    }


    private static final class TestWiring implements SchemaDirectiveWiring {
        private final List<RestDirective> directives;

        private TestWiring(final List<RestDirective> directives) {
            this.directives = directives;
        }

        @Override
        public GraphQLFieldDefinition onField(
            final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
            this.directives.add(RestDirective.fromArguments(environment.getDirective().getArguments()));
            return SchemaDirectiveWiring.super.onField(environment);
        }
    }

}
