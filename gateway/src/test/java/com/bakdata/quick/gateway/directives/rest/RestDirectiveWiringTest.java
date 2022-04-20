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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.gateway.GraphQLTestUtil;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethod;
import com.bakdata.quick.gateway.custom.type.RestDirectiveMethodType;
import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.fetcher.RestDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class RestDirectiveWiringTest {

    private static final Path workingDir = Path.of("", "src/test/resources/schema/directives/rest/wiring/");
    private final HttpClient httpClient = new HttpClient();

    private SchemaParser schemaParser;
    private SchemaGenerator schemaGenerator;
    private RuntimeWiring wiring;

    @BeforeEach
    void setUp() {
        this.schemaParser = new SchemaParser();
        this.schemaGenerator = new SchemaGenerator();
        this.wiring = RuntimeWiring.newRuntimeWiring()
            .directive("rest", new RestDirectiveWiring(this.httpClient))
            .build();
    }

    @Test
    void shouldParsePathArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).containsExactly(
                    new RestParameter("userId", true),
                    new RestParameter("recommendationType", true)
                );
                assertThat(fetcher.getQueryArguments()).isEmpty();
            });
    }

    @Test
    void shouldParseQueryArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).isEmpty();
                assertThat(fetcher.getQueryArguments()).containsExactlyInAnyOrder(
                    new RestParameter("limit", true),
                    new RestParameter("userId", true)
                );
            });
    }

    @Test
    void shouldParseMultipleArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).containsExactly(
                    new RestParameter("userId", true),
                    new RestParameter("recommendationType", true)
                );
                assertThat(fetcher.getQueryArguments()).containsExactlyInAnyOrder(
                    new RestParameter("limit", true),
                    new RestParameter("walks", true)
                );
            });
    }

    @Test
    void shouldParseOverriddenArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).containsExactly(
                    new RestParameter("userId", true),
                    new RestParameter("limit", true)
                );
                assertThat(fetcher.getQueryArguments()).containsExactlyInAnyOrder(
                    new RestParameter("recommendationType", true),
                    new RestParameter("walks", true)
                );
            });
    }

    @Test
    void shouldRespectOrderOfOverriddenArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).containsExactly(
                    new RestParameter("limit", true),
                    new RestParameter("userId", true)
                );
            });
    }

    @Test
    void shouldParseSpecifiedPathArguments(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
                assertThat(fetcher.getPathArguments()).containsExactly(
                    new RestParameter("recommendationType", true)
                );
                assertThat(fetcher.getQueryArguments()).containsExactly(
                    new RestParameter("limit", true),
                    new RestParameter("walks", true)
                );
            });
    }

    @Test
    void shouldParseEnum(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);

        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);

        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> assertThat(fetcher.getPathArguments()).contains(
                new RestParameter("recommendationType", true)
            ));
    }

    @Test
    void shouldDefaultToGetMethod(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);
        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);
        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .extracting(RestDataFetcher::getMethodType)
            .isEqualTo(RestDirectiveMethod.GET);
    }

    @Test
    void shouldParseHttpMethod(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);
        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);
        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .extracting(RestDataFetcher::getMethodType)
            .isEqualTo(RestDirectiveMethod.POST);
    }


    @Test
    void shouldParseBodyParameter(final TestInfo testInfo) throws IOException {
        final GraphQLSchema graphQLSchema = this.getGraphQLSchema(testInfo);
        final DataFetcher<?> restDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Query", "recommendation", graphQLSchema);
        assertThat(restDataFetcher)
            .asInstanceOf(InstanceOfAssertFactories.type(RestDataFetcher.class))
            .satisfies(fetcher -> {
               assertThat(fetcher.getBodyParameter()).isNotNull()
                   .isEqualTo(new RestParameter("inputs", false));
               assertThat(fetcher.getPathArguments()).isEmpty();
               assertThat(fetcher.getQueryArguments()).isEmpty();
            });
    }

    @Test
    void shouldNotAllowSameArgumentTwice(final TestInfo testInfo) throws IOException {
        assertThatExceptionOfType(QuickDirectiveException.class)
            .isThrownBy(() -> this.getGraphQLSchema(testInfo))
            .withMessageStartingWith("The argument(s) limit can't be used in multiple parameters");
    }

    @Test
    void shouldNotParseNonExistingPathArgument(final TestInfo testInfo) throws IOException {
        assertThatNullPointerException()
            .isThrownBy(() -> this.getGraphQLSchema(testInfo))
            .withMessageStartingWith("Argument nonExisting specified as parameter");
    }

    @Test
    void shouldNotParseNonExistingQueryArgument(final TestInfo testInfo) throws IOException {
        assertThatNullPointerException()
            .isThrownBy(() -> this.getGraphQLSchema(testInfo))
            .withMessageStartingWith("Argument nonExisting specified as parameter");
    }


    private GraphQLSchema getGraphQLSchema(final TestInfo testInfo) throws IOException {
        final String name = testInfo.getTestMethod().orElseThrow().getName();
        final String schemaString = Files.readString(workingDir.resolve(name + ".graphql"));
        final TypeDefinitionRegistry registry = this.schemaParser.parse(schemaString);
        registry.add(RestDirective.DEFINITION);
        registry.add(RestDirectiveMethodType.DEFINITION);
        return this.schemaGenerator.makeExecutableSchema(registry, this.wiring);
    }

}
