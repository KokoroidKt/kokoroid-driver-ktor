rootProject.name = "ktor-driver"

include(":core")
include(":api")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://www.jitpack.io") }
        mavenLocal()
    }

    plugins {
    }
}
