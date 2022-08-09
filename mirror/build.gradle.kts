description = "Mirrors let Quick efficiently query the content of topics. " +
        "They read the content of a topics and expose it through a REST API."

plugins {
    id("quick.base")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j)
    implementation(libs.log4j.over.slf4j)
    implementation(libs.jul.slf4j)
    implementation(libs.okhttp)
    implementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs
    implementation(libs.micronaut.rxjava)
    implementation(libs.spotbugs)
    implementation(libs.javax)
    implementation(libs.micronaut.inject)
    implementation(libs.caffeine)
    implementation(libs.streams.bootstrap)
    implementation(libs.kafka.proto.serde)
    implementation(libs.picocli)
    implementation(libs.micronaut.picocli)
    implementation(libs.micronaut.http.client)
    implementation(libs.micronaut.http.server)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.inject.java)
    testImplementation(libs.fluent.kafka.streams)
    testImplementation(libs.rest.assured)
    testImplementation(libs.schema.registry.mock)
    testImplementation(libs.micronaut.junit)
    testImplementation(libs.kafka.junit)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kafka)
    testImplementation(libs.awaitly)

    testImplementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs

    testRuntimeOnly(libs.junit.engine)
}
