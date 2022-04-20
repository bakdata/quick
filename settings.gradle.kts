pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "quick"

includeBuild("build-logic")

include("common")
include("gateway")
include("ingest")
include("manager")
include("mirror")
