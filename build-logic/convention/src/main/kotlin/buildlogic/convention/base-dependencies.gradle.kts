package buildlogic.convention

plugins {
    java
    id("buildlogic.libraries")
}

dependencies {
    implementation(libs.SLF4J_API)
    implementation(libs.LOG4J_API)
    implementation(libs.LOG4J_CORE)
    implementation(libs.LOG4J_SLF4J)
    implementation(libs.LOG4J_OVER_SLFJ)
    implementation(libs.JUL_SLF4J)
    implementation(libs.OK_HTTP_CLIENT)
    implementation(libs.JACKSON_DATABIND) // needed so that log4j2 can read yaml test configs
    implementation("io.micronaut.rxjava2:micronaut-rxjava2")
    implementation("com.github.spotbugs:spotbugs-annotations:4.7.1")
    implementation(libs.JAVAX)
    implementation(libs.INJECT)
    implementation(libs.CAFFEINE)

    annotationProcessor(libs.INJECT_JAVA)
    annotationProcessor(libs.VALIDATION)

    testAnnotationProcessor(libs.INJECT_JAVA)

    testImplementation(libs.MICRONAUT_JUNIT)
    testImplementation(libs.KAFKA_JUNIT)
    testImplementation(libs.JUNIT_API)
    testImplementation(libs.JUNIT_PARAMS)
    testImplementation(libs.ASSERTJ)
    testImplementation(libs.MOCKITO)
    testImplementation(libs.KAFKA)
    testImplementation(libs.AWAITLY)

    testImplementation(libs.JACKSON_DATABIND) // needed so that log4j2 can read yaml test configs

    testRuntimeOnly(libs.JUNIT_ENGINE)
}
