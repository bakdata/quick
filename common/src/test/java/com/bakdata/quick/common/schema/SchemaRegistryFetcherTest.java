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
import static org.assertj.core.api.Assertions.in;

import com.bakdata.quick.avro.Person;
import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.exception.HttpClientException;
import com.bakdata.quick.testutil.ProtoTestRecord;
import com.bakdata.schemaregistrymock.junit5.SchemaRegistryMockExtension;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.reactivex.Single;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class SchemaRegistryFetcherTest {

    public static final String TOPIC = "test-topic";
    @RegisterExtension
    final SchemaRegistryMockExtension srMock = new SchemaRegistryMockExtension();

    @Test
    void retrieveExistingAvroSchema() {
        final SchemaFetcher schemaFetcher = this.createSchemaFetcher(new AvroSchemaProvider());
        this.srMock.registerValueSchema(TOPIC, Person.getClassSchema());
        final ParsedSchema schema = schemaFetcher.getValueSchema(TOPIC).blockingGet();
        assertThat(schema.rawSchema()).isEqualTo(Person.getClassSchema());
    }

    @Test
    void retrieveExistingProtobufSchema() {
        final SchemaFetcher schemaFetcher = this.createSchemaFetcher(new ProtobufSchemaProvider());
        final ProtobufSchema protobufSchema = new ProtobufSchema(ProtoTestRecord.getDescriptor());
        this.srMock.registerValueSchema(TOPIC, protobufSchema);
        final ParsedSchema schema = schemaFetcher.getValueSchema(TOPIC).blockingGet();
        assertThat(schema).isEqualTo(protobufSchema);
    }

    @Test
    void retrieveExistingAvroSchemaForInternalTopic() {
        final SchemaFetcher schemaFetcher = this.createSchemaFetcher(new ProtobufSchemaProvider());
        final String internalTopic = "__" + TOPIC;
        this.srMock.registerValueSchema(internalTopic, Person.getClassSchema());
        final ParsedSchema schema = schemaFetcher.getValueSchema(internalTopic).blockingGet();
        assertThat(schema.rawSchema()).isEqualTo(Person.getClassSchema());
    }

    @Test
    void shouldReturnErrorIfSchemaDoesNotExist() throws InterruptedException {
        final SchemaFetcher schemaFetcher = this.createSchemaFetcher(new AvroSchemaProvider());
        final Single<ParsedSchema> valueSchema = schemaFetcher.getValueSchema(TOPIC);
        valueSchema.test().await()
            .assertError(HttpClientException.class)
            .assertErrorMessage("Not Found");
    }

    private SchemaFetcher createSchemaFetcher(final SchemaProvider schemaProvider) {
        return new SchemaRegistryFetcher(new HttpClient(), new KafkaConfig("dummy:123", this.srMock.getUrl()),
            schemaProvider);
    }
}
