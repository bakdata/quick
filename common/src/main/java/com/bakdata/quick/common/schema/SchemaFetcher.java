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

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.reactivex.Single;
import org.apache.avro.Schema;

/**
 * Client for interacting with the Avro Schema Registry.
 */
public interface SchemaFetcher {

    /**
     * Retrieves the value schema for a given topic.
     */
    Single<ParsedSchema> getValueSchema(final String topic);

    /**
     * Retrieves the key schema for a given topic.
     */
    Single<ParsedSchema> getKeySchema(final String topic);


    /**
     * Retrieves the schema for a given subject.
     *
     * <p>
     * Generally, a subject conforms to the following naming convetion: {@code <TOPIC_NAME>-<KEY|VALUE>},
     * e.g., topic1-key.
     */
    Single<ParsedSchema> getSchema(final String subject);

}
