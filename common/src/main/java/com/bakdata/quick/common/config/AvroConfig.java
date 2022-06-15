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

import com.bakdata.quick.common.condition.AvroSchemaFormatCondition;
import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Configuration regarding the converted GraphQL to Avro schemas.
 *
 * <p>
 * Moreover, it checks if the Avro namespace fulfills the naming convention in the specification.
 * </p>
 *
 * @see <a href="https://avro.apache.org/docs/current/spec.html">Avro Specification</a>
 */
@Requires(condition = AvroSchemaFormatCondition.class)
@ConfigurationProperties(AvroConfig.PREFIX)
@Getter
public class AvroConfig {
    static final String PREFIX = SchemaConfig.PREFIX + ".avro";
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[A-z_](\\.?\\w)*$");

    private final String namespace;

    /**
     * Default constructor.
     *
     * @param namespace The value of this field holds the name of the namespace where the object is stored.
     */
    @ConfigurationInject
    public AvroConfig(final String namespace) {
        if (!NAMESPACE_PATTERN.matcher(namespace).matches()) {
            throw new ConfigurationException(
                String.format("The Avro namespace %s does not fulfill the naming convention of Avro specification.",
                    namespace));
        }
        this.namespace = namespace;
    }

}
