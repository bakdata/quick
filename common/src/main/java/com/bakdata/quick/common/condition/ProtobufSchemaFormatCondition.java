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

package com.bakdata.quick.common.condition;

import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.schema.SchemaFormat;
import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;

/**
 * Condition for beans requiring the {@link SchemaFormat} to be Protobuf.
 *
 * @see SchemaConfig
 * @see com.bakdata.quick.common.config.ProtobufConfig
 */
public class ProtobufSchemaFormatCondition implements Condition {
    @Override
    public boolean matches(final ConditionContext context) {
        final SchemaConfig schemaConfig = context.getBean(SchemaConfig.class);
        return schemaConfig.isEnableAll() || schemaConfig.getFormat() == SchemaFormat.PROTOBUF;
    }
}
