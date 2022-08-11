import net.ltgt.gradle.errorprone.errorprone

description = "The gateway is Quick's GraphQL interface. " +
        "It lets you query and ingest data from, and to your Apache Kafka topics."

plugins {
    id("quick.base")
}

dependencies {
    implementation(libs.micronaut.graphql)
    implementation(libs.json2avro)
    implementation(libs.reactorKafka)
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4jImpl)
    implementation(libs.log4j.overSlf4j)
    implementation(libs.julSlf4j)
    implementation(libs.okhttp)
    implementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs
    implementation(libs.micronaut.rxjava)
    implementation(libs.spotbugs)
    implementation(libs.javax)
    implementation(libs.micronaut.inject)
    implementation(libs.caffeine)

    implementation(libs.guava)
    implementation(libs.graphql)
    implementation(libs.kafka.clients)
    implementation(libs.kubernetes.client)
    implementation(libs.schemaRegistryClient)
    implementation(libs.kafka.protobufProvider)
    implementation(libs.thymeleaf)
    implementation(libs.protobuf)
    implementation(libs.graphql.extendedScalars)
    implementation(libs.micronaut.httpClient)
    implementation(libs.micronaut.httpServer)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)
    implementation(libs.micronaut.security)

    annotationProcessor(libs.micronaut.injectJava)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.injectJava)

    testImplementation(libs.micronaut.inject)
    testImplementation(libs.avro)
    testImplementation(libs.schemaRegistryMock)
    testImplementation(libs.mockWebserver)
    testImplementation(libs.micronaut.rxjavaHttpClient)
    testImplementation(libs.micronaut.junit)
    testImplementation(libs.kafka.junit)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kafka)
    testImplementation(libs.awaitly)
    testImplementation(libs.apache.commons.lang)

    testImplementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs

    testRuntimeOnly(libs.junit.engine)
}

tasks.withType<JavaCompile>().configureEach {

    options.errorprone {

    }
}
