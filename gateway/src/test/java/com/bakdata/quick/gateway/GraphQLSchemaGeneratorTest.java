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

package com.bakdata.quick.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bakdata.quick.common.TestTopicRegistryClient;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.gateway.directives.QuickDirectiveException;
import com.bakdata.quick.gateway.fetcher.KeyFieldFetcher;
import com.bakdata.quick.gateway.fetcher.ListArgumentFetcher;
import com.bakdata.quick.gateway.fetcher.MutationFetcher;
import com.bakdata.quick.gateway.fetcher.QueryKeyArgumentFetcher;
import com.bakdata.quick.gateway.fetcher.QueryListFetcher;
import com.bakdata.quick.gateway.fetcher.subscription.MultiSubscriptionFetcher;
import com.bakdata.quick.gateway.fetcher.subscription.SubscriptionFetcher;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@MicronautTest(startApplication = false)
class GraphQLSchemaGeneratorTest {

    private static final Path workingDirectory = Path.of("src", "test", "resources", "schema", "conversion");
    private final GraphQLSchemaGenerator generator;
    private final TestTopicRegistryClient registryClient;

    @Inject
    GraphQLSchemaGeneratorTest(final GraphQLSchemaGenerator generator,
        final TestTopicRegistryClient registryClient) {
        this.generator = generator;
        this.registryClient = registryClient;
        this.registerTopics();
    }

    @Test
    void shouldConvertQueryWithSingleField(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");

        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        assertThat(schema.getTypeMap())
            .containsKeys("Purchase", "Price", "Query");

        final List<String> purchaseFields = schema.getObjectType("Purchase").getFieldDefinitions().stream()
            .map(GraphQLFieldDefinition::getName)
            .collect(Collectors.toList());

        assertThat(purchaseFields).containsExactly("purchaseId", "productId", "userId", "amount", "price", "infos");

        // check argument
        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Query", "findPurchase", schema);
        assertThat(fieldDefinition.getArgument("purchaseId"))
            .isNotNull()
            .extracting(GraphQLArgument::getType)
            .isInstanceOf(GraphQLScalarType.class)
            .hasFieldOrPropertyWithValue("name", "ID");

        // check that non-null fields are handled correctly
        final GraphQLFieldDefinition nonNonNullIdField =
            GraphQLTestUtil.getFieldDefinition("Purchase", "purchaseId", schema);
        assertThat(nonNonNullIdField)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLNonNull.class, list ->
                assertThat(list).extracting(GraphQLNonNull::getWrappedType).isEqualTo(Scalars.GraphQLID)
            );

        // check that list types are handled correctly
        final GraphQLFieldDefinition listField = GraphQLTestUtil.getFieldDefinition("Purchase", "infos", schema);
        assertThat(listField)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLList.class, list ->
                assertThat(list).extracting(GraphQLList::getWrappedType).isEqualTo(Scalars.GraphQLString));
        // check data fetcher is set correctly
        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "findPurchase", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);
    }

    @Test
    void shouldConvertQueryWithListArgument(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");

        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        assertThat(schema.getTypeMap())
            .containsKeys("Purchase", "Price", "Query");

        final List<String> purchaseFields = schema.getObjectType("Purchase").getFieldDefinitions().stream()
            .map(GraphQLFieldDefinition::getName)
            .collect(Collectors.toList());

        assertThat(purchaseFields).containsExactly("purchaseId", "productId", "userId", "amount", "price", "infos");

        // check argument
        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Query", "findPurchase", schema);
        assertThat(fieldDefinition.getArgument("purchaseId"))
            .isNotNull()
            .extracting(GraphQLArgument::getType)
            .isInstanceOfSatisfying(GraphQLList.class, list ->
                assertThat(list)
                    .extracting(GraphQLList::getWrappedType)
                    .hasFieldOrPropertyWithValue("name", "ID")
            );

        // check that non-null fields are handled correctly
        final GraphQLFieldDefinition nonNonNullIdField =
            GraphQLTestUtil.getFieldDefinition("Purchase", "purchaseId", schema);
        assertThat(nonNonNullIdField)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLNonNull.class, list ->
                assertThat(list).extracting(GraphQLNonNull::getWrappedType).isEqualTo(Scalars.GraphQLID)
            );

        // check that list types are handled correctly
        final GraphQLFieldDefinition listField = GraphQLTestUtil.getFieldDefinition("Purchase", "infos", schema);
        assertThat(listField)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLList.class, list ->
                assertThat(list).extracting(GraphQLList::getWrappedType).isEqualTo(Scalars.GraphQLString));
        // check data fetcher is set correctly
        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "findPurchase", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(ListArgumentFetcher.class);
    }

    @Test
    void shouldConvertQueryWithSingleFieldAndModification(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        assertThat(schema.getTypeMap())
            .containsKeys("Purchase", "Product", "Price", "Metadata", "Query");

        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Query", "findPurchase", schema);
        assertThat(fieldDefinition.getArgument("purchaseId"))
            .isNotNull()
            .extracting(GraphQLArgument::getType)
            .isInstanceOf(GraphQLScalarType.class)
            .hasFieldOrPropertyWithValue("name", "ID");

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "findPurchase", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);

        final DataFetcher<?> modificationFetcher = GraphQLTestUtil.getFieldDataFetcher("Purchase", "product", schema);
        assertThat(modificationFetcher)
            .isNotNull()
            .isInstanceOf(KeyFieldFetcher.class);
    }

    @Test
    void shouldConvertListQueryWithSingleFieldAndModification(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));
        assertThat(schema.getTypeMap())
            .containsKeys("Purchase", "Product", "Price", "Metadata", "Query");

        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Query", "findPurchases", schema);
        assertThat(fieldDefinition.getArguments())
            .isEmpty();

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "findPurchases", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryListFetcher.class);

        final DataFetcher<?> modificationFetcher = GraphQLTestUtil.getFieldDataFetcher("Purchase", "product", schema);
        assertThat(modificationFetcher)
            .isNotNull()
            .isInstanceOf(KeyFieldFetcher.class);
    }

    @Test
    void shouldConvertQueryWithPrimitiveType(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition fieldDefinition = GraphQLTestUtil.getFieldDefinition("Query", "getURL", schema);
        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .hasFieldOrPropertyWithValue("name", "String");

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "getURL", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);
    }

    @Test
    void shouldConvertQueryWithRange(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final List<GraphQLArgument> topicDirectiveArguments =
            GraphQLTestUtil.getTopicDirectiveArgumentsFromField("Query", "userRequests", schema);

        assertThat(topicDirectiveArguments)
            .hasSize(4)
            .extracting(GraphQLArgument::getName)
            .containsExactly("name", "keyArgument", "rangeFrom", "rangeTo");

        assertThat(topicDirectiveArguments)
            .hasSize(4)
            .extracting(GraphQLArgument::getValue)
            .containsExactly("user-request-range", "userId", "timestampFrom", "timestampTo");
    }

    @Test
    void shouldConvertQueryAllWithComplexType(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");

        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        assertThat(schema.getTypeMap()).containsKeys("TinyUrl", "TinyUrlCount", "Query");

        final GraphQLFieldDefinition fieldDefinition = GraphQLTestUtil.getFieldDefinition("Query", "fetchAll", schema);

        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLList.class, listType ->
                assertThat(listType.getWrappedType())
                    .isInstanceOf(GraphQLObjectType.class)
                    .hasFieldOrPropertyWithValue("name", "TinyUrl")
            );

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "fetchAll", schema);
        assertThat(rootDataFetcher).isNotNull().isInstanceOf(QueryListFetcher.class);
    }

    @Test
    void shouldConvertQueryAllWithPrimitiveType(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition fieldDefinition = GraphQLTestUtil.getFieldDefinition("Query", "getURL", schema);
        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLList.class, listType ->
                assertThat(listType.getWrappedType())
                    .isInstanceOf(GraphQLScalarType.class)
                    .hasFieldOrPropertyWithValue("name", "String")
            );

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "getURL", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryListFetcher.class);
    }

    @Test
    void shouldConvertQueryWithMultipleFields(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Query", "getProduct", schema);
        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .isInstanceOfSatisfying(GraphQLObjectType.class, objectType -> {
                assertThat(objectType.getFieldDefinition("url").getType())
                    .isInstanceOf(GraphQLScalarType.class)
                    .hasFieldOrPropertyWithValue("name", "String");

                assertThat(objectType.getFieldDefinition("product").getType())
                    .isInstanceOf(GraphQLObjectType.class)
                    .hasFieldOrPropertyWithValue("name", "Product");
            });

        final DataFetcher<?> urlFetcher = GraphQLTestUtil.getFieldDataFetcher("ProductInfo", "url", schema);
        assertThat(urlFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);

        final DataFetcher<?> productFetcher = GraphQLTestUtil.getFieldDataFetcher("ProductInfo", "product", schema);
        assertThat(productFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);
    }

    @Test
    void shouldConvertDefinitionWithSingleFieldAndObjectName(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        assertThat(schema.getType("UrlInfo"))
            .isNotNull()
            .isInstanceOfSatisfying(GraphQLObjectType.class, objectType ->
                assertThat(objectType.getFieldDefinition("url").getType())
                    .isNotNull()
                    .isInstanceOf(GraphQLScalarType.class)
                    .hasFieldOrPropertyWithValue("name", "String")
            );

        final DataFetcher<?> fieldFetcher = GraphQLTestUtil.getFieldDataFetcher("UrlInfo", "url", schema);
        assertThat(fieldFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);
    }

    @Test
    void shouldConvertIfMultipleValues(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        // check contract conversion
        assertThat(schema.getType("Contract"))
            .isNotNull()
            .isInstanceOfSatisfying(GraphQLObjectType.class, objectType -> {

                assertThat(objectType.getFieldDefinition("_id").getType())
                    .isNotNull()
                    .isInstanceOfSatisfying(GraphQLNonNull.class, nonNull ->
                        assertThat(nonNull.getWrappedType())
                            .hasFieldOrPropertyWithValue("name", "String")
                    );

                hasFieldWithListType(objectType, "policyHolderId", "PersonGrainValue");
                hasFieldWithListType(objectType, "insuredPersonId", "PersonGrainValue");
                hasFieldWithListType(objectType, "term", "GrainValue");
                hasFieldWithListType(objectType, "value", "GrainValue");
            });

        // check person conversion
        assertThat(schema.getType("Person"))
            .isNotNull()
            .isInstanceOfSatisfying(GraphQLObjectType.class, objectType -> {

                assertThat(objectType.getFieldDefinition("_id").getType())
                    .isNotNull()
                    .isInstanceOfSatisfying(GraphQLNonNull.class, nonNull ->
                        assertThat(nonNull.getWrappedType())
                            .hasFieldOrPropertyWithValue("name", "String")
                    );

                hasFieldWithListType(objectType, "firstname", "GrainValue");
                hasFieldWithListType(objectType, "lastname", "GrainValue");
            });

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Query", "findContract", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(QueryKeyArgumentFetcher.class);

        final DataFetcher<?> policyHolderFetcher =
            GraphQLTestUtil.getFieldDataFetcher("PersonGrainValue", "policyHolder", schema);
        assertThat(policyHolderFetcher)
            .isNotNull()
            .isInstanceOf(KeyFieldFetcher.class);
    }

    @Test
    void shouldConvertSubscription(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Subscription", "getURL", schema);
        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .hasFieldOrPropertyWithValue("name", "String");

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Subscription", "getURL", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(SubscriptionFetcher.class);
    }

    @Test
    void shouldConvertComplexSubscription(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition fieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Subscription", "subscribeProfile", schema);

        assertThat(fieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .hasFieldOrPropertyWithValue("name", "Profile");

        final DataFetcher<?> rootDataFetcher =
            GraphQLTestUtil.getFieldDataFetcher("Subscription", "subscribeProfile", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(MultiSubscriptionFetcher.class);
    }

    @Test
    void shouldConvertMutation(final TestInfo testInfo) throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");

        final GraphQLSchema schema = this.generator.create(Files.readString(schemaPath));

        final GraphQLFieldDefinition mutationFieldDefinition =
            GraphQLTestUtil.getFieldDefinition("Mutation", "setProduct", schema);
        assertThat(mutationFieldDefinition)
            .isNotNull()
            .extracting(GraphQLFieldDefinition::getType)
            .hasFieldOrPropertyWithValue("name", "Product");

        assertThat(mutationFieldDefinition.getArguments())
            .extracting(GraphQLArgument::getName)
            .containsExactlyInAnyOrder("id", "product");

        final DataFetcher<?> rootDataFetcher = GraphQLTestUtil.getFieldDataFetcher("Mutation", "setProduct", schema);
        assertThat(rootDataFetcher)
            .isNotNull()
            .isInstanceOf(MutationFetcher.class);
    }

    @Test
    void shouldNotConvertKeyFieldWithWrongKeyFieldName(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo,
            String.format("Could not find the keyField %s in the parent type definition. Please check your schema.",
                "notExistingField"));
    }

    @Test
    void shouldNotConvertKeyFieldWithWrongFieldType(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo,"Found unknown type: ListType");
    }

    @Test
    void shouldNotConvertIfMissingKeyInfoInQueryType(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo,
            "When the return type is not a list for a non-mutation and non-subscription type,"
                + " key information (keyArgument or keyField) is needed.");
    }

    @Test
    void shouldNotConvertIfMissingKeyInfoInBasicType(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo,
            "When the return type is not a list for a non-mutation and non-subscription type,"
                + " key information (keyArgument or keyField) is needed.");
    }

    @Test
    void shouldNotConvertIfMutationDoesNotHaveTwoArgs(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Mutation requires two input arguments");
    }

    @Test
    void shouldNotConvertIfKeyArgAndInputNameDifferentInQueryType(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Key argument has to be identical to the input name.");
    }

    @Test
    void shouldNotConvertIfKeyArgAndInputNameDifferentInNonQueryType(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Key argument has to be identical to the input name.");
    }

    @Test
    void shouldNotConvertIfRangeToArgumentIsMissing(final TestInfo testInfo) throws IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Both rangeFrom and rangeTo arguments should be set.");
    }

    @Test
    void shouldNotCovertIfRangeFromArgumentIsMissing(final TestInfo testInfo) throws  IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Both rangeFrom and rangeTo arguments should be set.");
    }

    @Test
    void shouldNotCovertIfRangeIsDefinedOnField(final TestInfo testInfo) throws  IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "Range queries are only supported on Query types.");
    }

    @Test
    void shouldNotCovertIfKeyArgumentInRangeQueryIsMissing(final TestInfo testInfo) throws  IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "You must define a keyArgument.");
    }

    @Test
    void shouldNotCovertIfReturnTypeOfRangeQueryIsNotList(final TestInfo testInfo) throws  IOException {
        this.assertQuickDirectiveExceptionMessage(testInfo, "The return type of range queries should be a list.");
    }

    private void registerTopics() {
        this.registryClient.register(
            "purchase-topic",
            new TopicData("purchase-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.AVRO, "")
        ).blockingAwait();

        this.registryClient.register(
            "product-topic",
            new TopicData("product-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.PROTOBUF, "")
        ).blockingAwait();

        this.registryClient.register(
            "contract-topic",
            new TopicData("contract-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.AVRO, "")
        ).blockingAwait();

        this.registryClient.register(
            "person-topic",
            new TopicData("person-topic", TopicWriteType.MUTABLE, QuickTopicType.DOUBLE, QuickTopicType.PROTOBUF, "")
        ).blockingAwait();

        this.registryClient.register(
            "url-topic",
            new TopicData("url-topic", TopicWriteType.MUTABLE, QuickTopicType.STRING, QuickTopicType.STRING, "")
        ).blockingAwait();
    }

    private void assertQuickDirectiveExceptionMessage(final TestInfo testInfo, final String message)
        throws IOException {
        final Path schemaPath = workingDirectory.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".graphql");
        final String schema = Files.readString(schemaPath);
        assertThatExceptionOfType(QuickDirectiveException.class)
            .isThrownBy(() -> this.generator.create(schema))
            .withMessage(message);
    }

    private static void hasFieldWithListType(final GraphQLObjectType objectType, final String insuredPersonId,
        final String fieldTypeName) {
        assertThat(objectType.getFieldDefinition(insuredPersonId).getType())
            .isNotNull()
            .isInstanceOfSatisfying(GraphQLList.class, list ->
                assertThat(list.getWrappedType())
                    .hasFieldOrPropertyWithValue("name", fieldTypeName)
            );
    }
}
