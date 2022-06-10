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

import com.bakdata.quick.common.condition.ProtobufSchemaFormatCondition;
import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import lombok.Getter;

/**
 * Configuration regarding the converted GraphQL to Protobuf schemas.
 */
@Requires(condition = ProtobufSchemaFormatCondition.class)
@ConfigurationProperties(ProtobufConfig.PREFIX)
@Getter
public class ProtobufConfig {
    static final String PREFIX = SchemaConfig.PREFIX + ".protobuf";
    private final String protobufPackage;

    /**
     * Default constructor.
     *
     * @param protobufPackage Holds the name of the protobuf package where the object is stored.
     */
    @ConfigurationInject
    public ProtobufConfig(@Property(name = PREFIX + ".package") final String protobufPackage) {
        this.protobufPackage = protobufPackage;
    }
}
