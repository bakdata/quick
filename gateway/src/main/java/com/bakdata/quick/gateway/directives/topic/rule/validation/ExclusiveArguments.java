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

package com.bakdata.quick.gateway.directives.topic.rule.validation;

import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import java.util.Optional;

/**
 * Validation for {@link com.bakdata.quick.gateway.directives.topic.TopicDirective}
 *
 * <p>
 * {@code keyArgument} and {@code keyField} are mutually exclusive because both specify where we obtain the key
 * from.
 */
public class ExclusiveArguments implements ValidationRule {
    @Override
    public Optional<String> validate(final TopicDirectiveContext context) {
        if (context.getTopicDirective().hasKeyField() && context.getTopicDirective().hasKeyArgument()) {
            return Optional.of("keyArgument and keyField are mutually exclusive.");
        }

        return Optional.empty();
    }
}
