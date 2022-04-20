package buildlogic.convention

plugins {
    java
    id("buildlogic.libraries")
}

val extension = extensions.create<HttpServiceExtension>("httpService")


dependencies {
    implementation(libs.HTTP_CLIENT)
    implementation(libs.HTTP_SERVER)
    implementation(libs.MICRONAUT_MANAGEMENT)
    implementation(libs.MICRONAUT_PROMETHEUS)

    if (extension.secured) {
        implementation(libs.SECURITY)
    }
}
