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

package buildlogic.libraries

abstract class QuickLibraries {
    /**
     * Definition of all dependencies used in Quick.
     */

    // ---------Logging---------
    val SLF4J_VERSION = "1.7.30"
    val LOG4J_VERSION = "2.17.2"

    val SLF4J_API = "org.slf4j:slf4j-api:$SLF4J_VERSION"
    val LOG4J_API = "org.apache.logging.log4j:log4j-api:$LOG4J_VERSION"
    val LOG4J_CORE = "org.apache.logging.log4j:log4j-core:$LOG4J_VERSION"
    val LOG4J_SLF4J = "org.apache.logging.log4j:log4j-slf4j-impl:$LOG4J_VERSION"
    val JUL_SLF4J = "org.slf4j:jul-to-slf4j:$SLF4J_VERSION"
    val LOG4J_OVER_SLFJ = "org.slf4j:log4j-over-slf4j:$SLF4J_VERSION"

    val JACKSON_DATABIND = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml" // required for reading config

    // ------Micronaut------
    val MICRONAUT_VERSION = "2.5.6"

    val JAVAX = "javax.annotation:javax.annotation-api"
    val HTTP_CLIENT = "io.micronaut:micronaut-http-client"
    val HTTP_SERVER = "io.micronaut:micronaut-http-server-netty"
    val INJECT = "io.micronaut:micronaut-inject"
    val VALIDATION = "io.micronaut:micronaut-validation"

    val INJECT_JAVA = "io.micronaut:micronaut-inject-java"
    val MICRONAUT_GRAPHQL = "io.micronaut.graphql:micronaut-graphql"
    val MICRONAUT_MANAGEMENT = "io.micronaut:micronaut-management"
    val MICRONAUT_PROMETHEUS = "io.micronaut.micrometer:micronaut-micrometer-registry-prometheus"

    val SECURITY = "io.micronaut.security:micronaut-security"

    val MICRONAUT_PICO_CLI = "io.micronaut.picocli:micronaut-picocli"

    // ------Kafka Ecosystem------
    val KAFKA_VERSION = "2.7.1"
    val STREAMS_BOOTSTRAP_VERSION = "1.7.0"
    val CONFLUENT_VERSION = "6.1.1"
    val AVRO_VERSION = "1.9.2"
    val PROTOBUF_VERSION = "3.20.1"
    val FLUENT_STREAMS_VERSION = "2.3.1"
    val REACTIVE_KAFKA_VERSION = "1.3.4"

    val KAFKA = "org.apache.kafka:kafka_2.13:$KAFKA_VERSION"
    val KAFKA_CLIENTS = "org.apache.kafka:kafka-clients:$KAFKA_VERSION"
    val KAFKA_STREAMS = "org.apache.kafka:kafka-streams:$KAFKA_VERSION"
    val STREAMS_BOOTSTRAP = "com.bakdata.kafka:streams-bootstrap:$STREAMS_BOOTSTRAP_VERSION"
    val AVRO = "org.apache.avro:avro:$AVRO_VERSION"
    val PROTOBUF = "com.google.protobuf:protobuf-java:$PROTOBUF_VERSION"
    val KAFKA_STREAMS_SERDE = "io.confluent:kafka-streams-avro-serde:$CONFLUENT_VERSION"
    val SCHEMA_REGISTRY_CLIENT = "io.confluent:kafka-schema-registry-client:$CONFLUENT_VERSION"
    val KAFKA_PROTOBUF_PROVIDER = "io.confluent:kafka-protobuf-provider:$CONFLUENT_VERSION"

    // ------Miscellaneous------
    val GUAVA_VERSION = "29.0-jre"
    val GRAPHQL_VERSION = "14.1"
    val JSON2AVRO_CONVERTER_VERSION = "0.2.9"
    val PICOCLI_VERSION = "4.2.0"
    val KUBE_MANAGER_VERSION = "5.9.0"
    val SUNDR_IO_VERSION = "0.50.2"
    val THYMELEAF_VERSION = "3.0.11.RELEASE"
    val OKHTTP_VERSION = "4.9.1"
    val CAFFEINE_VERSION = "3.0.2"

    val GUAVA = "com.google.guava:guava:$GUAVA_VERSION"
    val GRAPHQL = "com.graphql-java:graphql-java:$GRAPHQL_VERSION"
    val KUBE_MANAGER_CLIENT = "io.fabric8:kubernetes-client:$KUBE_MANAGER_VERSION"
    val SUNDR_IO = "io.sundr:builder-annotations:$SUNDR_IO_VERSION"
    val REACTIVE_KAFKA = "io.projectreactor.kafka:reactor-kafka:$REACTIVE_KAFKA_VERSION"
    val JSON2AVRO_CONVERTER = "tech.allegro.schema.json2avro:converter:$JSON2AVRO_CONVERTER_VERSION"
    val THYMELEAF = "org.thymeleaf:thymeleaf:$THYMELEAF_VERSION"
    val CAFFEINE = "com.github.ben-manes.caffeine:caffeine:$CAFFEINE_VERSION"
    val OK_HTTP_CLIENT = "com.squareup.okhttp3:okhttp:$OKHTTP_VERSION"
    val PICO_CLI = "info.picocli:picocli:$PICOCLI_VERSION"

    // ------Tests------
    val JUNIT_VERSION = "5.7.2"
    val MOCKITO_VERSION = "3.11.1"
    val ASSERTJ_VERSION = "3.20.1"
    val AWAITLY_VERSION = "4.1.0"
    val REST_ASSURED_VERSION = "4.2.0"
    val KAFKA_JUNIT_VERSION = "2.7.0" // generally same as Kafka, there are some exception however

    val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:$JUNIT_VERSION"
    val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:$JUNIT_VERSION"
    val JUNIT_PARAMS = "org.junit.jupiter:junit-jupiter-params:$JUNIT_VERSION"
    val ASSERTJ = "org.assertj:assertj-core:$ASSERTJ_VERSION"
    val MOCKITO = "org.mockito:mockito-junit-jupiter:$MOCKITO_VERSION"
    val AWAITLY = "org.awaitility:awaitility:$AWAITLY_VERSION"
    val MICRONAUT_JUNIT = "io.micronaut.test:micronaut-test-junit5"
    val FLUENT_KAFKA_STREAMS =
        "com.bakdata.fluent-kafka-streams-tests:fluent-kafka-streams-tests-junit5:$FLUENT_STREAMS_VERSION"
    val SCHEMA_REGISTRY_MOCK =
        "com.bakdata.fluent-kafka-streams-tests:schema-registry-mock-junit5:$FLUENT_STREAMS_VERSION"
    val KUBE_MOCK_SERVER = "io.fabric8:kubernetes-server-mock:$KUBE_MANAGER_VERSION"
    val REST_ASSURED = "io.rest-assured:rest-assured:$REST_ASSURED_VERSION"
    val OK_HTTP_MOCK_SERVER = "com.squareup.okhttp3:mockwebserver:$OKHTTP_VERSION"
    val KAFKA_JUNIT = "net.mguenther.kafka:kafka-junit:$KAFKA_JUNIT_VERSION"
    val RX_JAVA = "io.reactivex.rxjava2:rxjava" // required for testFixtures, otherwise included with micronaut
}
