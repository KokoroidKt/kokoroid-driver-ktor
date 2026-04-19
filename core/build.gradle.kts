import dev.kokoroidkt.gradle.runKokoroid.config.ExtensionTypes
import dev.kokoroidkt.gradle.runKokoroid.config.runKokoroid


plugins {
    kotlin("jvm")
    id("dev.kokoroidkt.gradle.runKokoroid") version "0.4.0"
    id("com.gradleup.shadow") version "9.4.1"
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "dev.kokoroid"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.ktor.client.mock)
    implementation(project(":api"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.kokoroidSuit)
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.ktorServer)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

runKokoroid {
    extensionFilename = "core-1.0-SNAPSHOT-all.jar"
    // i delete the github token, oops
    isValidationOnly = false
    skipDownload = true
    enableKokoroidDebug = true
    testExtensionType = ExtensionTypes.DRIVER
}
