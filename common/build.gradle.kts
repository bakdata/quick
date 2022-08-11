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
    implementation(libs.kafka.streams.avroSerde)
    implementation(libs.kafka.streams.protobufSerde)
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

    testFixturesImplementation(libs.kafka.streams)
    testFixturesImplementation(libs.kafka.streams.avroSerde)
    testFixturesImplementation(libs.avro)
    testFixturesImplementation(libs.micronaut.injectJava)
    testFixturesImplementation(libs.rxjava)
    testFixturesImplementation(libs.junit.api)
    testFixturesImplementation(libs.kafka.streams.protobufSerde)
    testFixturesImplementation(libs.protobuf)
    testFixturesImplementation(libs.spotbugs)

    testFixturesAnnotationProcessor(libs.micronaut.injectJava)

    testImplementation(testFixtures(project(":common")))
    testImplementation(libs.schemaRegistryMock)
    testImplementation(libs.mockWebserver)
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
