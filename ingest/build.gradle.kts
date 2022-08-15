description = "The ingest service is Quick's REST API for ingesting data into your Apache Kafka topics."

plugins {
    id("quick.base")
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4jImpl)
    implementation(libs.log4j.overSlf4j)
    implementation(libs.julSlf4j)
    implementation(libs.caffeine)
    implementation(libs.rxjava)
    implementation(libs.kafka.clients)
    implementation(libs.json2avro)
    implementation(libs.micronaut.httpClient)
    implementation(libs.micronaut.httpServer)
    implementation(libs.micronaut.management)
    implementation(libs.micronaut.prometheus)
    implementation(libs.micronaut.security)

    annotationProcessor(libs.micronaut.injectJava)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.injectJava)

    testImplementation(libs.schemaRegistryMock)
    testImplementation(libs.kafka.streams.protobufSerde)
    testImplementation(libs.micronaut.junit)
    testImplementation(libs.kafka.junit)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
    testImplementation(libs.kafka)
    testImplementation(libs.awaitly)
    testImplementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs
    testImplementation(libs.micronaut.rxjavaHttpClient)

    testRuntimeOnly(libs.junit.engine)
}
