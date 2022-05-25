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

package com.bakdata.quick.common.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.avro.Person;
import com.bakdata.quick.common.exception.HttpClientException;
import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import java.util.Map;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


@MicronautTest(rebuildContext = true)
class SchemaRegistryFetcherTest {

    public static final String TOPIC = "test-topic";
    @RegisterExtension
    final SchemaRegistryMockExtension srMock = new SchemaRegistryMockExtension();
    @Inject
    ApplicationContext applicationContext;

    @Test
    void retrieveExistingSchema() {
        this.applicationContext.environment(env -> env.addPropertySource("quick-test",
            Map.of("quick.kafka.schema-registry-url", this.srMock.getUrl())));

        final SchemaFetcher schemaFetcher = this.applicationContext.createBean(SchemaRegistryFetcher.class);
        this.srMock.registerValueSchema(TOPIC, Person.getClassSchema());
        final ParsedSchema schema = schemaFetcher.getValueSchema(TOPIC).blockingGet();
        assertThat(schema.rawSchema()).isEqualTo(Person.getClassSchema());
    }

    @Test
    void shouldReturnErrorIfSchemaDoesNotExist() throws InterruptedException {
        this.applicationContext.environment(env -> env.addPropertySource("quick-test",
            Map.of("quick.kafka.schema-registry-url", this.srMock.getUrl())));
        final SchemaFetcher schemaFetcher = this.applicationContext.createBean(SchemaRegistryFetcher.class);
        final Single<ParsedSchema> valueSchema = schemaFetcher.getValueSchema(TOPIC);
        valueSchema.test().await()
            .assertError(HttpClientException.class)
            .assertErrorMessage("Not Found");
    }
}
