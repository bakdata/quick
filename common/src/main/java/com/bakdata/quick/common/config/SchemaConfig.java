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


import com.bakdata.quick.common.schema.SchemaFormat;
import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Optional;
import lombok.Getter;

/**
 * Configurations for schemas used by Quick.
 */
@ConfigurationProperties(SchemaConfig.PREFIX)
@Getter
public class SchemaConfig {
    public static final String PREFIX = "quick.schema";
    public static final String SCHEMA_FORMAT = PREFIX + ".format";
    public static final SchemaFormat DEFAULT_FORMAT = SchemaFormat.AVRO;
    /**
     * Format of the schema to use in Quick.
     *
     * <p>
     * This is a global configuration that is set when deploying Quick. Quick will convert GraphQL schemas to this
     * format when user create a new topic, and register them with the Schema Registry.
     * </p>
     */
    private final SchemaFormat format;

    // Ideally, we'd like to make this optional and only to be created if "quick.schema.avro..." properties are set
    // It seems this isn't supported by micronaut though.
    // Therefore, all fields of avro config must be optional
    private final AvroConfig avro;

    @ConfigurationInject
    public SchemaConfig(final Optional<SchemaFormat> format, final AvroConfig avro) {
        this.format = format.orElse(DEFAULT_FORMAT);
        this.avro = avro;

        if (this.format == SchemaFormat.AVRO) {
            this.avro.validate();
        }
    }

}
