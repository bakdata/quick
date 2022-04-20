description = "Mirrors let Quick efficiently query the content of topics. " +
        "They read the content of a topics and expose it through a REST API."

plugins {
    id("quick.base")
    id("buildlogic.convention.http-service")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
}

httpService {
    secured = false
}

dependencies {
    implementation(libs.STREAMS_BOOTSTRAP)
    implementation(libs.PICO_CLI)
    implementation(libs.MICRONAUT_PICO_CLI)

    testImplementation(libs.FLUENT_KAFKA_STREAMS)
    testImplementation(libs.REST_ASSURED)
    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
}
