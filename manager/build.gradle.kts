description = "The manager lets you interact with all components of Quick. " +
        "It processes your requests for creating, modifying and deleting resources."

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

    compileOnly(libs.sundrIo)
    annotationProcessor(libs.sundrIo)
    annotationProcessor(libs.kube.manager.client)
    annotationProcessor(libs.micronaut.injectJava)
    annotationProcessor(libs.micronaut.validation)

    testAnnotationProcessor(libs.micronaut.injectJava)
    testImplementation(libs.kube.mock.server)
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

    testImplementation(libs.jackson.databind) // needed so that log4j2 can read yaml test configs

    testRuntimeOnly(libs.junit.engine)
}
