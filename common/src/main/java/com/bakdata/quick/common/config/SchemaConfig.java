package com.bakdata.quick.common.config;

import com.bakdata.quick.common.schema.SchemaFormat;
import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Optional;
import lombok.Getter;

/**
 * Configurations for schemas used by Quick.
 */
@ConfigurationProperties("quick.schema")
@Getter
public class SchemaConfig {
    public static final SchemaFormat DEFAULT_FORMAT = SchemaFormat.AVRO;
    /**
     * Format of the schema to use in Quick.
     *
     * <p>
     * This is a global configuration that is set when deploying Quick. Quick will convert GraphQL schemas
     * to this format when user create a new topic, and register them with the Schema Registry.
     * </p>
     */
    private final SchemaFormat format;

    @ConfigurationInject
    public SchemaConfig(final Optional<SchemaFormat> format) {
        this.format = format.orElse(DEFAULT_FORMAT);
    }
}
