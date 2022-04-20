plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "buildlogic"

repositories  {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("librariesPlugin") {
            id = "buildlogic.libraries"
            implementationClass = "buildlogic.libraries.QuickLibrariesPlugin"
        }
    }
}
