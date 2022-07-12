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

import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import com.bakdata.quick.gateway.directives.topic.rule.TopicDirectiveRules;
import java.util.List;
import java.util.Optional;

/**
 * Validates that a {@link com.bakdata.quick.gateway.directives.topic.TopicDirective} is used correctly.
 */
public class ValidationRules implements TopicDirectiveRules {
    private static final List<ValidationRule> VALIDATION_RULES;

    static {
        VALIDATION_RULES = List.of(
                new SubscriptionList(),
                new ExclusiveArguments(),
                new KeyInformation(),
                new MutationRequiresTwoArguments()
        );
    }

    @Override
    public void apply(final TopicDirectiveContext context) {
        final Optional<String> error = VALIDATION_RULES.stream()
            .map(rule -> rule.validate(context))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findAny();

        if (error.isPresent()) {
            throw new QuickDirectiveException(error.get());
        }
    }
}
