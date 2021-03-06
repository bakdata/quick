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

package com.bakdata.quick.common.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.bakdata.quick.common.ConfigUtils;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AvroConfigTest {

    @Test
    void shouldReadNamespace() {
        final Map<String, Object> properties = Map.of("quick.schema.avro.namespace", "test");
        final AvroConfig config = ConfigUtils.createWithProperties(properties, AvroConfig.class);
        assertThat(config.getNamespace()).isEqualTo("test");
    }

    @Test
    void shouldNotExistIfFormatIsProtobuf() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "protobuf");
        final Optional<AvroConfig> config;
        try (final ApplicationContext context = ConfigUtils.createWithProperties(properties)) {
            config = context.findBean(AvroConfig.class);
            assertThat(config).isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("provideCorrectAvroNamespaces")
    void shouldSetNamespaceWhenNameIsCorrect(final String input) {
        final AvroConfig avroConfig = new AvroConfig(input);
        assertThat(avroConfig.getNamespace()).isEqualTo(input);
    }

    @ParameterizedTest
    @MethodSource("provideWrongAvroNamespaces")
    void shouldThrowExceptionWhenNameIsWrong(final String input) {
        assertThatThrownBy(() -> new AvroConfig(input))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage(String.format(
                "The Avro namespace %s does not fulfill the naming convention of Avro specification.", input));
    }

    private static List<String> provideCorrectAvroNamespaces() {
        return List.of(
            "foo.bar",
            "foo.bar.baz",
            "foo_bar.baz",
            "foo_b8r.b4z",
            "Foo.Bar_baz"
        );
    }

    private static List<String> provideWrongAvroNamespaces() {
        return List.of(
            "8foo.bar",
            "foo..bar",
            ".foo.bar",
            "foo.bar-baz"
        );
    }
}
