description = "The manager lets you interact with all components of Quick. " +
        "It processes your requests for creating, modifying and deleting resources."

plugins {
    id("quick.base")
    id("buildlogic.convention.http-service")
}

dependencies {
    implementation(libs.GUAVA)
    implementation(libs.GRAPHQL)
    implementation(libs.KAFKA_CLIENTS)
    implementation(libs.KUBE_MANAGER_CLIENT)
    implementation(libs.SCHEMA_REGISTRY_CLIENT)
    implementation(libs.KAFKA_PROTOBUF_PROVIDER)
    implementation(libs.THYMELEAF)
    implementation(libs.PROTOBUF)
    implementation("com.graphql-java:graphql-java-extended-scalars:16.0.1")

    compileOnly(libs.SUNDR_IO)
    annotationProcessor(libs.SUNDR_IO)
    annotationProcessor(libs.KUBE_MANAGER_CLIENT)

    testImplementation(libs.KUBE_MOCK_SERVER)
    testImplementation(libs.SCHEMA_REGISTRY_MOCK)
    testImplementation(libs.OK_HTTP_MOCK_SERVER)

    testImplementation("io.micronaut.rxjava2:micronaut-rxjava2-http-client")
}
