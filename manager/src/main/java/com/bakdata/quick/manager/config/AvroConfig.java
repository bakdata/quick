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

package com.bakdata.quick.manager.config;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.exceptions.ConfigurationException;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.avro.AvroTypeException;

/**
 * Configuration regarding the converted GraphQL to Avro schemas. Moreover, it checks if the Avro namespace fulfills the
 * naming convention in the specification.
 *
 * @see <a href="https://avro.apache.org/docs/current/spec.html">Avro Specification</a>
 */
@ConfigurationProperties("quick.avro")
@Getter
public class AvroConfig {

    private static final Pattern namespacePattern = Pattern.compile("^[A-z_](\\.?[A-Za-z0-9_])*$");

    private final String namespace;

    /**
     * Default constructor.
     *
     * @param namespace The value of this field holds the name of the namespace where the object is stored.
     */
    @ConfigurationInject
    public AvroConfig(final String namespace) {
        if (!namespacePattern.matcher(namespace).matches()) {
            throw new ConfigurationException(
                String.format("The Avro namespace %s does not fulfill the naming convention of Avro specification.",
                    namespace));
        }
        this.namespace = namespace;
    }
}
