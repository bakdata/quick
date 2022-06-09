//package com.bakdata.quick.mirror;
//
//import com.bakdata.quick.common.TestConfigUtils;
//import com.bakdata.quick.common.TestTopicTypeService;
//import com.bakdata.quick.common.tags.IntegrationTest;
//import com.bakdata.quick.common.type.QuickTopicType;
//import com.bakdata.quick.common.type.TopicTypeService;
//import com.bakdata.quick.mirror.base.HostConfig;
//import com.bakdata.quick.mirror.service.QueryServiceContext;
//import com.bakdata.schemaregistrymock.SchemaRegistryMock;
//import io.micronaut.context.ApplicationContext;
//import io.micronaut.context.annotation.Property;
//import io.micronaut.http.HttpStatus;
//import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
//import lombok.extern.slf4j.Slf4j;
//import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
//import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
//import org.apache.kafka.streams.KafkaStreams;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import javax.inject.Inject;
//import javax.inject.Provider;
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//
//import static io.restassured.RestAssured.when;
//import static net.mguenther.kafka.junit.EmbeddedKafkaCluster.provisionWith;
//import static org.awaitility.Awaitility.await;
//import static org.hamcrest.Matchers.equalTo;
//import static org.mockito.Mockito.mock;
//
//@MicronautTest
//@Property(name = "pod.ip", value = "127.0.0.1")
//public class StreamsAppStateControllerTest {
//
//    public static final String INPUT_TOPIC = "input";
//    private final SchemaRegistryMock schemaRegistry = new SchemaRegistryMock();
//    @Inject
//    HostConfig hostConfig;
//    @Inject
//    ApplicationContext applicationContext;
//    private EmbeddedKafkaCluster kafkaCluster;
//
//    @BeforeEach
//    void setup() {
//        this.kafkaCluster = provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
//        this.kafkaCluster.start();
//        this.schemaRegistry.start();
//    }
//
//    @AfterEach
//    void teardown() {
//        this.schemaRegistry.stop();
//        this.kafkaCluster.stop();
//    }
//
//    @Test
//    void x() {
//
//        MirrorApplication<String, String> app = setUpApp();
//        KafkaStreams streams = app.getStreams();
//
//        final Map<Integer, String> expected = Map.of(1, "1");
//        await().atMost(Duration.ofSeconds(10))
//                .untilAsserted(() -> when()
//                        .get("http://127.0.0.1:22623/mirror/partitions")
//                        .then()
//                        .statusCode(HttpStatus.OK.getCode())
//                        .body(equalTo(expected)));
//    }
//
//    private TopicTypeService topicTypeService() {
//        return TestTopicTypeService.builder()
//                .urlSupplier(this.schemaRegistry::getUrl)
//                .keyType(QuickTopicType.STRING)
//                .valueType(QuickTopicType.STRING)
//                .keySchema(null)
//                .valueSchema(null)
//                .build();
//    }
//
//    private MirrorApplication<String, String> setUpApp() {
//        final MirrorApplication<String, String> app = new MirrorApplication<>(
//                this.applicationContext,
//                this.topicTypeService(),
//                TestConfigUtils.newQuickTopicConfig(),
//                this.hostConfig
//        );
//        app.setInputTopics(List.of(INPUT_TOPIC));
//        app.setBrokers(this.kafkaCluster.getBrokerList());
//        app.setProductive(false);
//        app.setSchemaRegistryUrl(this.schemaRegistry.getUrl());
//        return app;
//    }
//}
