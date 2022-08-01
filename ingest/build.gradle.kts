description = "The ingest service is Quick's REST API for ingesting data into your Apache Kafka topics."

plugins {
    id("quick.base")
    id("buildlogic.convention.http-service")
}

dependencies {
    implementation(libs.KAFKA_CLIENTS)
    implementation(libs.JSON2AVRO_CONVERTER)

    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
    testImplementation(libs.KAFKA_PROTO_SERDE)

    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2-http-client")
}
