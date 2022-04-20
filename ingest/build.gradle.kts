description = "The ingest service is Quick's REST API for ingesting data into your Apache Kafka topics."

plugins {
    id("quick.base")
    id("buildlogic.convention.http-service")
}

dependencies {
    implementation(libs.KAFKA_CLIENTS)
    implementation(libs.JSON2AVRO_CONVERTER)

    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
}
