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

package com.bakdata.quick.gateway.custom.type;

import graphql.language.Description;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import jakarta.inject.Singleton;

/**
 * Custom enum for specifying the supported HTTP methods by the rest directive.
 *
 * <p>
 * Corresponds to the following GraphQL definition:
 * <pre>{@code
 * enum RestDirectiveMethod {
 *     GET, POST
 * }
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.directives.rest.RestDirective
 */
@Singleton
public class RestDirectiveMethodType implements QuickGraphQLType<EnumTypeDefinition> {
    public static final EnumTypeDefinition DEFINITION;
    public static final String TYPE_NAME = "RestDirectiveMethod";
    public static final String ENUM_DESCRIPTION = "Supported HTTP methods by the rest directive.";

    static {
        final EnumTypeDefinition.Builder builder = EnumTypeDefinition.newEnumTypeDefinition()
            .name(TYPE_NAME)
            // sourceLocation and multiline aren't used anywhere anyway
            .description(new Description(ENUM_DESCRIPTION, null, false));

        for (final RestDirectiveMethod method : RestDirectiveMethod.values()) {
            final EnumValueDefinition valueDefinition = EnumValueDefinition.newEnumValueDefinition()
                .name(method.name())
                .build();
            builder.enumValueDefinition(valueDefinition);
        }

        DEFINITION = builder.build();
    }

    @Override
    public EnumTypeDefinition getDefinition() {
        return DEFINITION;
    }

}
