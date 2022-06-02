package com.bakdata.quick.common.config;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.exceptions.ConfigurationException;
import java.util.regex.Pattern;
import lombok.Getter;

@ConfigurationProperties(ProtobufConfig.PREFIX)
@Getter
public class ProtobufConfig {
    public static final String PREFIX = SchemaConfig.PREFIX + ".proto";
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z_](\\.?\\w)*$");

    private final String protobufPackage;

    /**
     * Default constructor.
     *
     * @param protobufPackage The value of this field holds the name of the namespace where the object is stored.
     */
    @ConfigurationInject
    public ProtobufConfig(final String protobufPackage) {
        if (!NAMESPACE_PATTERN.matcher(protobufPackage).matches()) {
            throw new ConfigurationException(
                String.format(
                    "The Protobuf package %s does not fulfill the naming convention of Protobuf specification.",
                    protobufPackage));
        }
        this.protobufPackage = protobufPackage;
    }
}
