import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin

plugins {
    kotlin("jvm") version "2.3.10" apply false
    id("java")
    id("com.gradleup.nmcp.aggregation").version("1.4.4")
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "dev.kokoroid"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val docVersion = "0.x"

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)

        // Output directory for additional files
        // Use this block instead of the standard when you
        // want to change the output directory and include extra files
        outputDirectory.set(rootDir.resolve("docs/api/$docVersion"))

        // Use fileTree to add multiple files
        includes.from(
            fileTree("docs") {
                include("**/*.md")
            },
        )
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:2.2.0")
    }
}

dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin:2.2.0")
    dokka(project(":api"))
    nmcpAggregation(project(":api"))
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin:2.2.0")
}

tasks.dokkaHtml {
    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = docVersion
        versionsOrdering = listOf(project.version.toString())
        renderVersionsNavigationOnAllPages = true
    }
}

allprojects {
    group = "dev.kokoroidkt"
    version = findProperty("version")?.toString()
        ?: System.getenv("VERSION")
        ?: "undefined"
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

nmcpAggregation {
    centralPortal {
        username = providers.environmentVariable("MAVEN_USERNAME").orNull
        password = providers.environmentVariable("MAVEN_PASSWORD").orNull

        // optional: publish manually from the portal
        publishingType = "USER_MANAGED"

        // optional: configure the name of your publication in the portal UI
        publicationName = "kokoroid:$version"
    }
}
