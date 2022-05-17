package com.bakdata.quick.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import com.bakdata.quick.common.schema.SchemaFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaConfigTest {

    @Test
    void shouldCreateConfigWithProtobuf() {
        testProperty("quick.schema.format", "protobuf", SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigFromCaps() {
        testProperty("quick.schema.format", "PROTOBUF", SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigWithAvro() {
        testProperty("quick.schema.format", "avro", SchemaFormat.AVRO);
    }

    @Test
    void shouldCreateConfigWithDefault() {
        testProperty("something.other", "config", SchemaFormat.AVRO);
    }

    @Test
    void shouldCreateConfigFromEnvVariable() {
        testEnvironment("QUICK_SCHEMA_FORMAT", "protobuf", SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigFromEnvVariableCaps() {
        testEnvironment("QUICK_SCHEMA_FORMAT", "PROTOBUF", SchemaFormat.PROTOBUF);
    }

    @Test
    void shouldCreateConfigFromEnvVariableAvro() {
        testEnvironment("QUICK_SCHEMA_FORMAT", "avro", SchemaFormat.AVRO);
    }

    private static void testProperty(final String key, final String value, final SchemaFormat expected) {
        final SchemaConfig schemaConfig = ConfigUtils.createWithProperties(Map.of(key, value), SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(expected);
    }

    private static void testEnvironment(final String key, final String value, final SchemaFormat expected) {
        final SchemaConfig schemaConfig = ConfigUtils.createWithEnvironment(Map.of(key, value), SchemaConfig.class);
        assertThat(schemaConfig.getFormat()).isEqualTo(expected);
    }

}
