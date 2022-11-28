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

package com.bakdata.quick.mirror;

import com.bakdata.kafka.CleanUpRunner;
import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.kafka.util.ImprovedAdminClient;
import com.bakdata.quick.common.api.model.TopicWriteType;
import com.bakdata.quick.common.config.KafkaConfig;
import com.bakdata.quick.common.config.QuickTopicConfig;
import com.bakdata.quick.common.config.SchemaConfig;
import com.bakdata.quick.common.exception.BadArgumentException;
import com.bakdata.quick.common.resolver.StringResolver;
import com.bakdata.quick.common.schema.SchemaFormat;
import com.bakdata.quick.common.type.QuickTopicData;
import com.bakdata.quick.common.type.QuickTopicData.QuickData;
import com.bakdata.quick.common.type.QuickTopicType;
import com.bakdata.quick.common.type.TopicTypeService;
import com.bakdata.quick.common.util.CliArgHandler;
import com.bakdata.quick.mirror.base.HostConfig;
import com.bakdata.quick.mirror.base.QuickTopologyData;
import com.bakdata.quick.mirror.context.MirrorContext;
import com.bakdata.quick.mirror.context.MirrorContext.MirrorContextBuilder;
import com.bakdata.quick.mirror.context.MirrorContextProvider;
import com.bakdata.quick.mirror.context.RangeIndexProperties;
import com.bakdata.quick.mirror.context.RetentionTimeProperties;
import com.bakdata.quick.mirror.range.extractor.type.AvroTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.type.ProtoTypeExtractor;
import com.bakdata.quick.mirror.range.extractor.value.GenericRecordValueExtractor;
import com.bakdata.quick.mirror.range.extractor.value.MessageValueExtractor;
import com.bakdata.quick.mirror.topology.MirrorTopology;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.micronaut.runtime.server.EmbeddedServer;
import io.reactivex.Single;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Kafka Streams application and REST service for mirror applications.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Setter
@Singleton
@Slf4j
public class MirrorApplication<K, V> extends KafkaStreamsApplication {
    public static final String POINT_STORE = "mirror-store";
    public static final String RETENTION_STORE = "retention-store";
    public static final String RANGE_STORE = "range-store";

    // injectable parameter
    private final TopicTypeService topicTypeService;
    private final QuickTopicConfig topicConfig;
    private final ApplicationContext context;
    private final HostConfig hostConfig;
    private final MirrorContextProvider<K, V> contextProvider;

    // CLI Arguments
    @Option(names = "--store-type", description = "Kafka Store to use. Choices: ${COMPLETION-CANDIDATES}",
        defaultValue = "inmemory")
    private final StoreType storeType = StoreType.INMEMORY;
    @Nullable
    @Option(names = "--retention-time", description = "Retention time defined in ISO_8601")
    private Duration retentionTime;

    @Nullable
    @Option(names = "--range-field", description = "The field which the Mirror builds its range index on")
    private String rangeField;

    /**
     * Constructor.
     *
     * @param context Micronaut application context
     * @param topicTypeService Quick's topic type service
     * @param topicConfig kafka topic config
     * @param hostConfig host config for this pod
     */
    public MirrorApplication(final ApplicationContext context, final TopicTypeService topicTypeService,
        final QuickTopicConfig topicConfig, final HostConfig hostConfig,
        final MirrorContextProvider<K, V> contextProvider) {
        this.topicTypeService = topicTypeService;
        this.topicConfig = topicConfig;
        this.context = context;
        this.hostConfig = hostConfig;
        this.contextProvider = contextProvider;
    }

    public static void main(final String[] args) {
        startWithWebServer(Micronaut.run(args), args);
    }

    @Override
    public void buildTopology(final StreamsBuilder builder) {
        // not used because we need to change the topology itself
        // see createTopology()
    }

    @Override
    public Topology createTopology() {
        final MirrorContext<K, V> mirrorContext = this.buildTopologyContext();

        return new MirrorTopology<>(mirrorContext).createTopology();
    }

    private MirrorContext<K, V> buildTopologyContext() {
        final MirrorContextBuilder<K, V> builder = MirrorContext.<K, V>builder()
            .quickTopologyData(this.getTopologyData())
            .pointStoreName(POINT_STORE)
            .storeType(this.storeType)
            .rangeIndexProperties(new RangeIndexProperties(RANGE_STORE, this.rangeField))
            .retentionTimeProperties(new RetentionTimeProperties(RETENTION_STORE, this.retentionTime))
            .isCleanup(this.cleanUp);

        final SchemaConfig schemaConfig = this.context.getBean(SchemaConfig.class);
        if (schemaConfig.getFormat() == SchemaFormat.PROTOBUF) {
            builder.fieldTypeExtractor(new ProtoTypeExtractor());
            builder.fieldValueExtractor(new MessageValueExtractor<>());
        } else if (schemaConfig.getFormat() == SchemaFormat.AVRO) {
            builder.fieldTypeExtractor(new AvroTypeExtractor());
            builder.fieldValueExtractor(new GenericRecordValueExtractor<>());
        }

        this.contextProvider.setMirrorContext(builder.build());
        return builder.build();
    }

    @Override
    public String getUniqueAppId() {
        return this.getClass().getSimpleName() + "-" + this.getInputTopics().get(0);
    }

    /**
     * Starts application with embedded HTTP server runtime.
     *
     * @param context current context
     * @param args CLI arguments
     */
    private static void startWithWebServer(final ApplicationContext context, final String[] args) {
        // do NOT use try resource block, as it closes the application context and with the server
        final String[] allArgs = addKafkaConfigToArgs(context, args);
        final int exitCode = execute(context, allArgs);

        if (exitCode != 0) {
            context.getBean(MirrorApplication.class).close();
            log.error("Problem in starting mirror application.");
        }
    }

    /**
     * Adds Kafka specific configurations to CLI args
     *
     * <p>
     * {@link KafkaStreamsApplication} handles reading common settings like bootstrap server and schema registry url for
     * us. With this, we enable it to properly populate these.
     *
     * @param context current context
     * @param args existing CLI args
     */
    private static String[] addKafkaConfigToArgs(final ApplicationContext context, final String[] args) {
        final KafkaConfig kafkaConfig = context.getBean(KafkaConfig.class);
        final List<String> allArgs = CliArgHandler.convertArgs(kafkaConfig);
        allArgs.addAll(Arrays.asList(args));
        return allArgs.toArray(String[]::new);
    }

    /**
     * Starts application within given context.
     *
     * @param context application context to start streams app in
     * @param args CLI arguments
     */
    private static int execute(final ApplicationContext context, final String[] args) {
        return new CommandLine(MirrorApplication.class, new MicronautFactory(context))
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);
    }

    @Override
    protected void runStreamsApplication() {
        final MirrorContext<K, V> mirrorContext = this.contextProvider.get();

        final MirrorContextBuilder<K, V> builder = mirrorContext.toBuilder()
            .streams(this.getStreams())
            .hostInfo(this.hostConfig.toInfo());

        this.contextProvider.setMirrorContext(builder.build());

        log.debug("Built query service context {}", this.contextProvider.get());
        super.runStreamsApplication();
    }

    @Override
    protected Properties createKafkaProperties() {
        final Properties properties = super.createKafkaProperties();

        // disable exactly once (sets everything back to the default settings)
        properties.setProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        properties.put(StreamsConfig.producerPrefix(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION), 5);

        // sets this also back to default
        properties.setProperty(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "1");

        // required for distributed interactive queries
        // for more info, see https://medium.com/bakdata/queryable-kafka-topics-with-kafka-streams-8d2cca9de33f
        properties.setProperty(StreamsConfig.APPLICATION_SERVER_CONFIG, this.hostConfig.toConnectionString());

        log.info("Application Server Config: {}", this.hostConfig.toConnectionString());

        // cast to int is required, otherwise Kafka's config complains about it
        properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, (int) this.topicConfig.getReplicationFactor());
        return properties;
    }

    @Override
    protected void closeResources() {
        this.context.findBean(EmbeddedServer.class).ifPresent(EmbeddedServer::stop);
    }

    /**
     * Returns data for the input topic.
     *
     * @return data for the input topic
     */
    private QuickTopologyData<K, V> getTopologyData() {
        // during clean up, the topic might already be deleted from the registry (see method's java doc)
        if (this.isCleanUp()) {
            return this.cleanUpTopicData();
        }

        // query the topic registry for getting information about the topic and set it during runtime
        final String inputTopic = this.getInputTopics().get(0);
        final Single<QuickTopicData<K, V>> topicDataFuture = this.topicTypeService.getTopicData(inputTopic);
        final QuickTopicData<K, V> topicData = topicDataFuture
            .onErrorResumeNext(e -> {
                final String message = String.format("Could not find %s in registry: %s", inputTopic, e.getMessage());
                return Single.error(new BadArgumentException(message));
            })
            .blockingGet();

        return QuickTopologyData.<K, V>builder()
            .inputTopics(this.getInputTopics())
            .outputTopic(this.getOutputTopic())
            .errorTopic(this.getErrorTopic())
            .topicData(topicData)
            .build();
    }

    /**
     * Return static topic data for a cleanup run.
     *
     * <p>
     * When the cleanup is executed, it is possible that the topic is already deleted from the topic registry.
     * Therefore, the type service cannot be called. Instead, we return static data (not respecting actual types). This
     * works because the cleanup requires only the topic names!
     *
     * @return fallback topic data for clean up run
     */
    @SuppressWarnings("unchecked") // ok since conversion does not happen during clean up
    private QuickTopologyData<K, V> cleanUpTopicData() {
        final QuickData<String> data =
            new QuickData<>(QuickTopicType.STRING, Serdes.String(), new StringResolver(), null);
        return (QuickTopologyData<K, V>) QuickTopologyData.<String, String>builder()
            .inputTopics(this.getInputTopics())
            .outputTopic(this.getOutputTopic())
            .errorTopic(this.errorTopic)
            .topicData(new QuickTopicData<>(this.getInputTopics().get(0), TopicWriteType.MUTABLE, data, data))
            .build();
    }

    @Override
    protected void cleanUpRun(final CleanUpRunner cleanUpRunner) {
        try {
            super.cleanUpRun(cleanUpRunner);
            // clean up runner does not take care of internal topics
            try (final ImprovedAdminClient kafkaClient = cleanUpRunner.getAdminClient()) {
                kafkaClient.getAdminClient().deleteTopics(this.getInputTopics());
                this.getInputTopics()
                    .forEach(topic -> kafkaClient.getSchemaTopicClient().resetSchemaRegistry(topic));
            }
        } catch (final RuntimeException e) {
            log.warn("Could not run clean up successfully", e);
            // force exit so that it will be rerun by k8s
            System.exit(1);
        }
    }
}
