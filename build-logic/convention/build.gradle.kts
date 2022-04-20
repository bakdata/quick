plugins {
    `kotlin-dsl`
}

group = "buildlogic"

repositories  {
    mavenCentral()
    gradlePluginPortal()
}


dependencies {
    implementation(project(":libraries"))
    implementation("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.1.4")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")
    implementation("io.freefair.gradle:lombok-plugin:6.1.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
    implementation("com.adarshr:gradle-test-logger-plugin:3.0.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
}

gradlePlugin {
    plugins {
        create("reporterPlugin") {
            id = "quick.reporter"
            implementationClass = "buildlogic.convention.ReporterPlugin"
        }

        create("basePlugin") {
            id = "quick.base"
            implementationClass = "buildlogic.convention.BasePlugin"
        }
    }
}
