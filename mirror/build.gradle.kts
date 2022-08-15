description = "Mirrors let Quick efficiently query the content of topics. " +
        "They read the content of a topics and expose it through a REST API."

plugins {
    id("quick.base")
    alias(libs.plugins.gradleAvroPlugin)
}

dependencies {
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
    implementation(libs.streamsBootstrap)
    implementation(libs.kafka.streams.protobufSerde)
    implementation(libs.micronaut.picocli)
    implementation(libs.micronaut.httpClient)
    implementation(libs.micronaut.httpServer)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)

    annotationProcessor(libs.micronaut.injectJava)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.injectJava)
    testImplementation(libs.fluentKafkaStreams)
    testImplementation(libs.micronaut.restAssured)
    testImplementation(libs.schemaRegistryMock)
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
