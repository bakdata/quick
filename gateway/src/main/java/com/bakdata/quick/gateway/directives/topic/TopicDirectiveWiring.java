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

package com.bakdata.quick.gateway.directives.topic;

import com.bakdata.quick.gateway.directives.QuickDirectiveWiring;
import com.bakdata.quick.gateway.directives.topic.rule.TopicDirectiveRules;
import com.bakdata.quick.gateway.directives.topic.rule.fetcher.DataFetcherRules;
import com.bakdata.quick.gateway.directives.topic.rule.validation.ValidationRules;
import com.bakdata.quick.gateway.fetcher.FetcherFactory;
import graphql.language.DirectiveDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import jakarta.inject.Singleton;

/**
 * Wiring for handling topic directives when processing the GraphQL schema.
 *
 * @see TopicDirective
 */
@Singleton
public class TopicDirectiveWiring implements QuickDirectiveWiring {
    private final FetcherFactory fetcherFactory;
    private final TopicDirectiveRules validationRules;
    private final TopicDirectiveRules fetcherRules;

    /**
     * Default constructor.
     */
    public TopicDirectiveWiring(final FetcherFactory fetcherFactory) {
        this.fetcherFactory = fetcherFactory;
        this.validationRules = new ValidationRules();
        this.fetcherRules = new DataFetcherRules();
    }

    @Override
    public GraphQLFieldDefinition onField(final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        final TopicDirective directive = TopicDirective.fromArguments(environment.getDirective().getArguments());
        final TopicDirectiveContext context = new TopicDirectiveContext(environment, directive, this.fetcherFactory);
        // make sure all topic directives are in valid locations
        this.validationRules.apply(context);
        // update data fetcher for all fields
        this.fetcherRules.apply(context);

        return environment.getElement();
    }

    @Override
    public DirectiveDefinition getDefinition() {
        return TopicDirective.DEFINITION;
    }

    @Override
    public String getName() {
        return TopicDirective.DIRECTIVE_NAME;
    }
}
