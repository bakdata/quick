import net.ltgt.gradle.errorprone.errorprone

description = "The gateway is Quick's GraphQL interface. " +
        "It lets you query and ingest data from, and to your Apache Kafka topics."

plugins {
    id("quick.base")
    id("buildlogic.convention.http-service")
}

dependencies {
    implementation(libs.MICRONAUT_GRAPHQL)
    implementation(libs.SCHEMA_REGISTRY_CLIENT)
    implementation(libs.REACTIVE_KAFKA)
    implementation(libs.JSON2AVRO_CONVERTER)
    implementation(libs.GUAVA)
    implementation(libs.KAFKA_PROTO_SERDE)
    implementation("com.graphql-java:graphql-java-extended-scalars:16.0.1")

    testImplementation(libs.AVRO)
    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
    testImplementation(libs.OK_HTTP_MOCK_SERVER)
    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2-http-client")
}

tasks.withType<JavaCompile>().configureEach {

    options.errorprone {

    }
}
