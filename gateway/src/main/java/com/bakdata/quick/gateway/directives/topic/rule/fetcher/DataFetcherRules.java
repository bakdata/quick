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

package com.bakdata.quick.gateway.directives.topic.rule.fetcher;

import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import com.bakdata.quick.gateway.directives.topic.rule.TopicDirectiveRules;
import graphql.schema.GraphQLCodeRegistry;
import java.util.List;

/**
 * Rules for setting custom data fetcher based on a topic directive.
 */
public class DataFetcherRules implements TopicDirectiveRules {
    private static final List<DataFetcherRule> rules;

    static {
        rules = List.of(
            new SubscriptionRule(),
            new QueryFetcherRule(),
            new QueryListFetchRule(),
            new ListArgumentFetcherRule(),
            new ModificationRule(),
            new ModificationListRule(),
            new MutationRule()
        );
    }

    @Override
    public void apply(final TopicDirectiveContext context) {
        final GraphQLCodeRegistry.Builder codeRegistry = context.getEnvironment().getCodeRegistry();
        rules.stream()
            .filter(rule -> rule.isValid(context))
            .flatMap(rule -> rule.extractDataFetchers(context).stream())
            .forEach(spec -> codeRegistry.dataFetcher(spec.getCoordinates(), spec.getDataFetcher()));
    }
}
