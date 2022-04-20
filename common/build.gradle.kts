plugins {
    id("com.github.davidmc24.gradle.plugin.avro") version "1.2.0"
    id("quick.base")
}

quick {
    type = quick.LIBRARY
}

dependencies {
    implementation(libs.GUAVA)
    implementation(libs.GRAPHQL)
    implementation(libs.AVRO)
    implementation(libs.KAFKA_STREAMS)
    implementation(libs.KAFKA_STREAMS_SERDE)
    implementation(libs.JSON2AVRO_CONVERTER)
    implementation(libs.HTTP_CLIENT)
    implementation(libs.HTTP_SERVER)
    implementation(libs.MICRONAUT_MANAGEMENT)
    implementation(libs.MICRONAUT_PROMETHEUS)
    implementation(libs.SECURITY)

    testFixturesImplementation(libs.KAFKA_STREAMS)
    testFixturesImplementation(libs.KAFKA_STREAMS_SERDE)
    testFixturesImplementation(libs.AVRO)
    testFixturesImplementation(libs.INJECT_JAVA)
    testFixturesImplementation(libs.RX_JAVA)
    testFixturesImplementation(libs.JUNIT_API)

    testFixturesAnnotationProcessor(libs.INJECT_JAVA)

    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
    testImplementation(libs.OK_HTTP_MOCK_SERVER)
}
