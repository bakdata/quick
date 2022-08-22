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

package com.bakdata.quick.manager.topic;

import static com.bakdata.quick.common.api.model.KeyValueEnum.KEY;
import static com.bakdata.quick.common.api.model.KeyValueEnum.VALUE;

import com.bakdata.quick.common.api.client.GatewayClient;
import com.bakdata.quick.common.api.client.TopicRegistryClient;
import com.bakdata.quick.common.api.model.KeyValueEnum;
import com.bakdata.quick.common.api.model.TopicData;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.api.model.manager.GatewaySchema;
import com.bakdata.quick.common.api.model.manager.creation.MirrorCreationData;
import com.bakdata.quick.common.api.model.manager.creation.TopicCreationData;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.QuickTopicConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.manager.gateway.GatewayService;
import com.bakdata.quick.manager.graphql.GraphQLConverter;
import com.bakdata.quick.manager.mirror.MirrorService;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.reactivex.Completable;
import io.reactivex.Single;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Service for creating Quick topics in Kafka.
 */
@Singleton
@Slf4j
public class KafkaTopicService implements TopicService {
    private static final int CAPACITY = 1_000;

    private final TopicRegistryClient topicRegistryClient;
    private final GatewayClient gatewayClient;
    private final GraphQLConverter graphQLConverter;
    private final SchemaRegistryClient schemaRegistryClient;
    private final MirrorService mirrorService;
    private final GatewayService gatewayService;
    private final QuickTopicConfig topicConfig;
    private final KafkaConfig kafkaConfig;

    /**
     * Injectable constructor.
     *
     * @param topicRegistryClient client for interacting with Quick's topic registry
     * @param gatewayClient client for interacting with Quick's gateways
     * @param graphQLConverter converter from GraphQL schema to Kafka schema
     * @param mirrorService service for creating mirrors
     * @param gatewayService service for interacting with deployed gateway
     * @param topicConfig configuration for Kafka topics
     * @param kafkaConfig configuration for Kafka
     */

    public KafkaTopicService(final TopicRegistryClient topicRegistryClient, final GatewayClient gatewayClient,
        final GraphQLConverter graphQLConverter, final MirrorService mirrorService,
        final GatewayService gatewayService, final QuickTopicConfig topicConfig,
        final KafkaConfig kafkaConfig) {
        this.topicRegistryClient = topicRegistryClient;
        this.gatewayClient = gatewayClient;
        this.graphQLConverter = graphQLConverter;
        this.mirrorService = mirrorService;
        this.gatewayService = gatewayService;
        this.topicConfig = topicConfig;
        this.kafkaConfig = kafkaConfig;
        this.schemaRegistryClient = new CachedSchemaRegistryClient(kafkaConfig.getSchemaRegistryUrl(), CAPACITY);
    }

    @Override
    public Single<List<TopicData>> getTopicList() {
        return this.topicRegistryClient.getAllTopics();
    }

    @Override
    public Single<TopicData> getTopicData(final String name) {
        return this.topicRegistryClient.getTopicData(name);
    }

    @SuppressWarnings("RxReturnValueIgnored")
    @Override
    public Completable createTopic(final String name, final QuickTopicType keyType, final QuickTopicType valueType,
        final TopicCreationData topicCreationData) {
        log.info("Create new topic {} with data {}", name, topicCreationData);
        // we don't need the cache, so make sure we get the current information
        this.schemaRegistryClient.reset();

        // first, check if topic might already exist in topic registry or kafka itself and then make also sure there
        final Completable kafkaStateCheck =
            Completable.mergeArray(this.checkKafka(name), this.checkTopicRegistry(name));

        final Single<Optional<QuickSchemas>> keySchema = this.getQuickSchemas(topicCreationData.getKeySchema()).cache();
        final Single<Optional<QuickSchemas>> valueSchema =
            this.getQuickSchemas(topicCreationData.getValueSchema()).cache();

        final Completable schemaRegistryCheck = Completable.defer(() -> Completable.mergeArray(
            keySchema.flatMapCompletable(schema -> this.checkSchemaRegistry(name + "-key", schema)),
            valueSchema.flatMapCompletable(schema -> this.checkSchemaRegistry(name + "-value", schema))
        ));

        final Completable stateCheck = kafkaStateCheck.andThen(schemaRegistryCheck);

        // create topic in kafka and deploy a mirror application
        final Completable kafkaTopicCreation = this.createKafkaTopic(name);
        final Completable mirrorCreation = this.createMirror(name, topicCreationData.getRetentionTime(),
            topicCreationData.isPoint(),
            topicCreationData.getRangeFiled());

        // default to mutable topic write type
        final TopicWriteType writeType =
            Objects.requireNonNullElse(topicCreationData.getWriteType(), TopicWriteType.MUTABLE);
        // register at topic registry (value schema can be nullable)
        // todo evaluate whether the schema should be part of the topic registry
        final Completable topicRegister = Completable.defer(() -> {
            log.debug("Register subject '{}' with topic registry", name);
            return valueSchema.flatMapCompletable(schema -> {
                final String graphQLSchema = schema.map(QuickSchemas::getGraphQLSchema).orElse(null);
                final TopicData topicData = new TopicData(name, writeType, keyType, valueType, graphQLSchema);
                return this.topicRegistryClient.register(name, topicData);
            });
        });

        // register potential avro schema with the schema registry
        final Completable keyRegister =
            keySchema.flatMapCompletable(schemas -> this.registerSchema(name, schemas, KEY));
        final Completable valueRegister =
            valueSchema.flatMapCompletable(schemas -> this.registerSchema(name, schemas, VALUE));

        final Completable creationOperations = Completable.mergeArray(
            kafkaTopicCreation,
            mirrorCreation,
            topicRegister,
            keyRegister,
            valueRegister
        );
        return stateCheck.andThen(creationOperations.doOnError(ignored -> this.deleteTopic(name)));
    }

    @Override
    public Completable deleteTopic(final String name) {
        return Completable.defer(() -> {
            log.debug("Delete topic {}", name);

            // we don't need the cache, so make sure we get the current information
            this.schemaRegistryClient.reset();
            // deleting stuff that has to do with Kafka happens during the cleanup run
            // the cleanup run is a job that is deployed when deleting the mirror
            final Completable deleteMirror = this.deleteMirror(name);
            final Completable deleteFromRegistry = this.topicRegistryClient.delete(name);
            return deleteMirror.andThen(deleteFromRegistry);
        });
    }

    private static Completable checkExistence(final boolean exists, final Supplier<String> errorSupplier) {
        if (exists) {
            final String errorMessage = errorSupplier.get();
            log.debug(errorMessage);
            return Completable.error(new BadArgumentException(errorMessage));
        } else {
            return Completable.complete();
        }
    }

    private Completable createKafkaTopic(final String topicName) {
        return Completable.defer(() -> {
            log.debug("Create Kafka topic {}", topicName);
            try (final AdminClient client = AdminClient.create(this.kafkaConfig.asProps())) {
                final int partitions = this.topicConfig.getPartitions();
                final short replicationFactor = this.topicConfig.getReplicationFactor();
                final NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
                return Completable.fromFuture(client.createTopics(List.of(newTopic)).all());
            }
        });
    }

    private Completable createMirror(final String topicName,
        @Nullable final Duration retentionTime,
        final boolean point,
        @Nullable final String rangeField) {
        return Completable.defer(() -> {
            log.debug("Create mirror for topic {}", topicName);
            final MirrorCreationData mirrorCreationData = new MirrorCreationData(topicName,
                topicName,
                1,
                null, // use default tag
                retentionTime,
                point,
                rangeField);
            return this.mirrorService.createMirror(mirrorCreationData);
        });
    }

    private Completable checkSchemaRegistry(final String subject, final Optional<QuickSchemas> schema) {
        if (schema.isEmpty()) {
            return Completable.complete();
        }

        return Single.fromCallable(() ->
                // if subject exists, we have to check compatibility
                this.schemaRegistryClient.getAllSubjects().contains(subject)
                    && this.schemaRegistryClient.testCompatibility(subject, schema.get().getParsedSchema())
            )
            .retry(3, IOException.class::isInstance)
            .flatMapCompletable(incompatibleSchema ->
                checkExistence(incompatibleSchema,
                    () -> String.format("Subject \"%s\" already exists", subject))
            );
    }

    private Completable checkKafka(final String name) {
        return Single.fromCallable(() -> {
                try (final AdminClient adminClient = AdminClient.create(this.kafkaConfig.asProps())) {
                    return adminClient.listTopics().names();
                }
            })
            .flatMap(Single::fromFuture)
            .map(topics -> topics.contains(name))
            .flatMapCompletable(exists ->
                checkExistence(exists, () -> String.format("Topic \"%s\" already exists in Kafka", name))
            );
    }

    private Completable checkTopicRegistry(final String name) {
        return this.topicRegistryClient.topicDataExists(name)
            .flatMapCompletable(exists ->
                checkExistence(exists, () -> String.format("Topic \"%s\" already exists in Registry", name))
            );
    }

    private Completable deleteMirror(final String topicName) {
        return Completable.fromAction(() -> log.debug("Delete mirror for topic {}", topicName))
            .mergeWith(this.mirrorService.deleteMirror(topicName));
    }

    private Completable registerSchema(final String topic, final Optional<QuickSchemas> schemas,
        final KeyValueEnum keyValue) {
        // if there is no schema, we can just return
        if (schemas.isEmpty()) {
            return Completable.complete();
        }
        // otherwise parse the string as schema and send it to the schema registry
        return Completable.fromAction(() -> {
                final String subject = keyValue.asSubject(topic);
                log.debug("Register subject '{}' with schema registry", subject);
                this.schemaRegistryClient.register(subject, schemas.get().getParsedSchema());
            }
        );
    }

    private Single<Optional<QuickSchemas>> getQuickSchemas(final GatewaySchema gatewaySchema) {
        if (gatewaySchema == null) {
            return Single.just(Optional.empty());
        }

        // make sure the gateway exists
        return this.gatewayService.getGateway(gatewaySchema.getGateway())
            .flatMap(ignored -> this.gatewayClient.getWriteSchema(gatewaySchema.getGateway(), gatewaySchema.getType()))
            .map(schemaResponse -> {
                final String graphQLSchema = schemaResponse.getSchema();
                final ParsedSchema parsedSchema = this.graphQLConverter.convert(graphQLSchema);
                return Optional.of(new QuickSchemas(graphQLSchema, parsedSchema));
            });
    }

    @Value
    private static class QuickSchemas {
        String graphQLSchema;
        ParsedSchema parsedSchema;
    }
}
