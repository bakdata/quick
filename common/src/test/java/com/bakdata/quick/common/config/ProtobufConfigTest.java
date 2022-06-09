package com.bakdata.quick.common.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.bakdata.quick.common.ConfigUtils;
import io.micronaut.context.ApplicationContext;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProtobufConfigTest {

    @Test
    void shouldReadNamespace() {
        final Map<String, Object> properties =
            Map.of("quick.schema.format", "protobuf", "quick.schema.protobuf.protobuf-package", "test");
        final ProtobufConfig config = ConfigUtils.createWithProperties(properties, ProtobufConfig.class);
        assertThat(config.getProtobufPackage()).isEqualTo("test");
    }

    @Test
    void shouldNotExistIfFormatIsProtobuf() {
        final Map<String, Object> properties = Map.of("quick.schema.format", "avro");
        final Optional<ProtobufConfig> config;
        try (final ApplicationContext context = ConfigUtils.createWithProperties(properties)) {
            config = context.findBean(ProtobufConfig.class);
            assertThat(config).isEmpty();
        }
    }
}
