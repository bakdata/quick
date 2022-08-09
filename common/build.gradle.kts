plugins {
    alias(libs.plugins.gradleAvroPlugin)
    id("quick.base")
    id("quick.protobuf.generator")
}

quick {
    type = quick.LIBRARY
}

dependencies {
    implementation(libs.json2avro)
    implementation(libs.avro)
    implementation(libs.kafka.streams.serde)
    implementation(libs.kafka.proto.serde)
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

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.inject.java)

    testFixturesImplementation(libs.kafka.streams)
    testFixturesImplementation(libs.kafka.streams.serde)
    testFixturesImplementation(libs.avro)
    testFixturesImplementation(libs.micronaut.inject.java)
    testFixturesImplementation(libs.rxjava)
    testFixturesImplementation(libs.junit.api)
    testFixturesImplementation(libs.kafka.proto.serde)
    testFixturesImplementation(libs.protobuf)
    testFixturesImplementation(libs.spotbugs)

    testFixturesAnnotationProcessor(libs.micronaut.inject.java)

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.schema.registry.mock)
    testImplementation(libs.mockwebserver)
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
