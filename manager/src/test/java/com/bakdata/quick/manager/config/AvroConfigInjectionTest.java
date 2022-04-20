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

package com.bakdata.quick.manager.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "quick.avro.namespace", value = "foo.bar.test.avro")
class AvroConfigInjectionTest {
    @Inject
    private AvroConfig avroConfig;

    @Test
    void shouldInjectConfigWithCorrectNamespace() {
        assertThat(this.avroConfig.getNamespace()).isEqualTo("foo.bar.test.avro");
    }
}
