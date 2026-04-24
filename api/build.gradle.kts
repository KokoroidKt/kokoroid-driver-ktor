plugins {
    kotlin("jvm")
    id("com.gradleup.nmcp")
    `maven-publish`
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jetbrains.dokka-javadoc")
    signing
}
repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.kokoroidSuit)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
}

kotlin {
    jvmToolchain(21)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    archiveFileName.set("kokoroidkt-adapter-api-$version-sources.jar")
    from(sourceSets.main.get().allSource)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Kokoroid Http Driver API",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "dev.kokoroidkt",
            "Add-Opens" to "java.base/java.lang java.base/jdk.internal.loader",
            "Add-Exports" to "java.base/jdk.internal.loader",
            "Enable-Native-Access" to "ALL-UNNAMED",
        )
    }
    archiveFileName.set("kokoroidkt-driver-api-$version.jar")
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc) // 依赖生成文档的任务
    archiveClassifier.set("javadoc")
    archiveFileName.set("kokoroidkt-adapter-api-$version-javadoc.jar")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJavadocJar)
            pom {
                name.set("Kokoroid Http Driver API")
                description.set("Http Driver API module of Kokoroid framework")
                artifactId = "kokoroidkt-adapter-api"
                url.set("https://github.com/KokoroidKt/kokoroid-driver-ktor")
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, version 2.1")
                        url.set("https://www.gnu.org/licenses/lgpl-2.1.txt")
                    }
                }
                developers {
                    developer {
                        id.set("kokoroidkt")
                        name.set("Kokoroid Dev Team")
                    }
                    developer {
                        id.set("moran0710")
                        name.set("Moran0710")
                        email.set("moran0710@qq.com")
                    }
                }
                scm {
                    url.set("https://github.com/KokoroidKt/kokoroid-driver-ktor.git")
                    connection.set("scm:git:git://github.com/KokoroidKt/kokoroid-driver-ktor.git")
                    developerConnection.set("scm:git:ssh://git@github.com/KokoroidKt/kokoroid-driver-ktor.git")
                }
            }
        }
    }
}

signing {
    val keyId = System.getenv("GPG_KEY_ID")
    val password = System.getenv("GPG_PASSWORD")
    val keyContent = System.getenv("GPG_PRIVATE_KEY")

    if (!keyContent.isNullOrBlank() && !password.isNullOrBlank()) {
        logger.lifecycle("Using in-memory GPG keys from environment variables.")
        if (!keyId.isNullOrBlank()) {
            useInMemoryPgpKeys(keyId, keyContent, password)
        } else {
            useInMemoryPgpKeys(keyContent, password)
        }
    } else {
        logger.lifecycle("Using local GPG keyring (~/.gnupg) for signing.")
        // 如果你的本地 key 已经在默认 keyring 里，这里通常不需要额外配置
        // 但前提是 Gradle 能找到可用的签名 key
        useGpgCmd()
    }

    sign(publishing.publications["mavenJava"])
}

tasks.test {
    useJUnitPlatform()
}
