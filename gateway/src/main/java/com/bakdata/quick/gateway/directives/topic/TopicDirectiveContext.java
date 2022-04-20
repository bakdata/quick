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

import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.fetcher.FetcherFactory;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import lombok.Value;

/**
 * Context for processing a {@link TopicDirective}.
 */
@Value
@SuppressWarnings("ObjectToString") // Lombok does that for us
public class TopicDirectiveContext {
    SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment;
    TopicDirective topicDirective;
    boolean isListType;
    boolean isNullable;
    boolean hasNullableElements;
    String parentContainerName;
    GraphQLOutputType type;
    FetcherFactory fetcherFactory;

    /**
     * Default constructor.
     */
    public TopicDirectiveContext(final SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment,
        final TopicDirective topicDirective,
        final FetcherFactory fetcherFactory
    ) {
        this.environment = environment;
        this.topicDirective = topicDirective;
        this.fetcherFactory = fetcherFactory;
        this.parentContainerName = environment.getNodeParentTree().getParentInfo()
            .map(parent -> parent.getNode().getName())
            .orElseThrow(() -> new QuickDirectiveException("Topic directive must have parent"));
        this.type = environment.getElement().getType();
        this.isListType = GraphQLTypeUtil.isList(GraphQLTypeUtil.unwrapNonNull(this.type));
        this.isNullable = GraphQLTypeUtil.isNullable(this.type);
        this.hasNullableElements = GraphQLTypeUtil.isNullable(GraphQLTypeUtil.unwrapAll(this.type));
    }
}
