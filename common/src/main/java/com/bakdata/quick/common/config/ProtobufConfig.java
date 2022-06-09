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

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;

/**
 * Configuration regarding the converted GraphQL to Protobuf schemas.
 *
 */
@ConfigurationProperties(ProtobufConfig.PREFIX)
@Getter
public class ProtobufConfig {
    private static final String PROTO_FILE_EXTENSION = ".proto";
    static final String PREFIX = SchemaConfig.PREFIX + PROTO_FILE_EXTENSION;
    private final String protobufPackage;

    /**
     * Default constructor.
     *
     * @param protobufPackage Holds the name of the protobuf package where the object is stored.
     */
    @ConfigurationInject
    public ProtobufConfig(final String protobufPackage) {
        this.protobufPackage = protobufPackage;
    }
}
