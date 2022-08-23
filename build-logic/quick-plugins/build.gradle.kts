plugins {
    `kotlin-dsl`
}

group = "buildlogic"

repositories  {
    mavenCentral()
    gradlePluginPortal()
}


dependencies {
    implementation("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.2.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.12.RELEASE")
    implementation("io.freefair.gradle:lombok-plugin:6.1.0")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.4.0.2513")
    implementation("com.adarshr:gradle-test-logger-plugin:3.2.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.19")
}

gradlePlugin {
    plugins {
        create("reporterPlugin") {
            id = "quick.reporter"
            implementationClass = "buildlogic.quickplugins.ReporterPlugin"
        }

        create("basePlugin") {
            id = "quick.base"
            implementationClass = "buildlogic.quickplugins.BasePlugin"
        }

        create("protobufPlugin") {
            id = "quick.protobuf.generator"
            implementationClass = "buildlogic.quickplugins.ProtobufGeneratorPlugin"
        }
    }
}
