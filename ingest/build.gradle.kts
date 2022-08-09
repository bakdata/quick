description = "The ingest service is Quick's REST API for ingesting data into your Apache Kafka topics."

plugins {
    id("quick.base")
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j)
    implementation(libs.log4j.over.slf4j)
    implementation(libs.jul.slf4j)
    implementation(libs.caffeine)
    implementation(libs.rxjava)
    implementation(libs.kafka.clients)
    implementation(libs.json2avro)
    implementation(libs.micronaut.http.client)
    implementation(libs.micronaut.http.server)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)
    implementation(libs.security)

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.inject.java)

    testImplementation(libs.schema.registry.mock)
    testImplementation(libs.kafka.proto.serde)
    testImplementation(libs.micronaut.junit)
    testImplementation(libs.kafka.junit)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kafka)
    testImplementation(libs.awaitly)
    testImplementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs
    testImplementation(libs.micronaut.rxjava.http.client)

    testRuntimeOnly(libs.junit.engine)
}
