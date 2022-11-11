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

import com.bakdata.quick.gateway.DataFetcherSpecification;
import com.bakdata.quick.gateway.directives.topic.TopicDirectiveContext;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;


/**
 * Rule for mutation fetcher. We are assuming that the first input argument (i.e., id) in the Mutation type field the
 * key is and the second input (i.e., product) argument of the Mutation type field the value is.
 *
 * <p>
 * <h2>Example:</h2>
 * <pre>{@code
 * Input ProductInput {
 *  id: ID!
 *  name: String!
 * }
 *
 * type Product {
 *  id: ID!
 *  name: String!
 * }
 *
 * type Query {
 *  getProduct(id: ID): Product @topic(name: "product-topic", keyArgument= "id")
 * }
 *
 * type Mutation {
 *  setProduct(id: ID, product: ProductInput): Product @topic(name: "product-topic")
 * }
 *
 * }</pre>
 *
 * @see com.bakdata.quick.gateway.fetcher.MutationFetcher
 */
@Slf4j
public class MutationRule implements DataFetcherRule {

    @Override
    public List<DataFetcherSpecification> extractDataFetchers(final TopicDirectiveContext context) {
        final List<DataFetcherSpecification> specifications = new ArrayList<>();

        final GraphQLArgument keyInputArgument = context.getEnvironment().getFieldDefinition().getArguments().get(0);
        final GraphQLArgument valueInputArgument = context.getEnvironment().getFieldDefinition().getArguments().get(1);
        log.trace("Extracting a mutation fetcher for key {}", keyInputArgument);
        log.trace("Extracting a mutation fetcher for value {}", valueInputArgument);

        final String topicName = context.getTopicDirective().getTopicName();

        final DataFetcher<?> dataFetcher = context.getFetcherFactory().mutationFetcher(
            topicName,
            keyInputArgument.getName(),
            valueInputArgument.getName()
        );

        final FieldCoordinates coordinates = this.currentCoordinates(context);
        specifications.add(DataFetcherSpecification.of(coordinates, dataFetcher));

        return specifications;
    }

    @Override
    public boolean isValid(final TopicDirectiveContext context) {
        log.trace("validating mutation rule.");
        final List<GraphQLArgument> arguments = context.getEnvironment().getFieldDefinition().getArguments();
        return context.getParentContainerName().equals("Mutation")
            && arguments.size() == 2
            && arguments.get(0) != null
            && arguments.get(1) != null;
    }
}
