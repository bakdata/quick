description = "The manager lets you interact with all components of Quick. " +
        "It processes your requests for creating, modifying and deleting resources."

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
    implementation(libs.kube.manager.client)
    implementation(libs.schema.registry.client)
    implementation(libs.kafka.protobuf.provider)
    implementation(libs.thymeleaf)
    implementation(libs.protobuf)
    implementation(libs.graphql.extended.scalars)
    implementation(libs.micronaut.http.client)
    implementation(libs.micronaut.http.server)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)
    implementation(libs.security)

    compileOnly(libs.sundr.io)
    annotationProcessor(libs.sundr.io)
    annotationProcessor(libs.kube.manager.client)
    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.inject.java)
    testImplementation(libs.kube.mock.server)
    testImplementation(libs.schema.registry.mock)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.micronaut.rxjava.http.client)
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
