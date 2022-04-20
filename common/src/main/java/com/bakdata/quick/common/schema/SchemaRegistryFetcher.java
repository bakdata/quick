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

import static com.bakdata.quick.common.api.model.KeyValueEnum.KEY;
import static com.bakdata.quick.common.api.model.KeyValueEnum.VALUE;

import com.bakdata.quick.common.api.client.HttpClient;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.exception.HttpClientException;
import io.micronaut.http.HttpStatus;
import io.reactivex.Single;
import javax.inject.Singleton;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;

/**
 * Client for retrieving Avro schemas from the schema registry.
 */
@Singleton
public class SchemaRegistryFetcher implements SchemaFetcher {
    private final HttpClient client;
    private final String schemaRegistryUrl;

    /**
     * Default constructor.
     */
    public SchemaRegistryFetcher(final HttpClient client, final KafkaConfig kafkaConfig) {
        this.client = client;
        this.schemaRegistryUrl = kafkaConfig.getSchemaRegistryUrl();
    }

    @Override
    public Single<Schema> getValueSchema(final String topic) {
        return this.getSchema(VALUE.asSubject(topic));
    }

    @Override
    public Single<Schema> getKeySchema(final String topic) {
        return this.getSchema(KEY.asSubject(topic));
    }

    @Override
    public Single<Schema> getSchema(final String subject) {
        final Request build = new Request.Builder()
            .url(String.format("%s/subjects/%s/versions/latest", this.schemaRegistryUrl, subject))
            .header("Content-Type", "application/vnd.schemaregistry.v1+json")
            .build();

        final Parser parser = new Parser();
        return Single.fromCallable(() -> {
            try (final Response response = this.client.newCall(build).execute()) {
                if (response.code() != HttpStatus.OK.getCode()) {
                    throw new HttpClientException(HttpStatus.valueOf(response.code()));
                }
                return parser.parse(this.client.objectMapper().readValue(response.body().byteStream(),
                    io.confluent.kafka.schemaregistry.client.rest.entities.Schema.class).getSchema());
            }
        });
    }
}
