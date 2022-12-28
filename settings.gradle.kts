rootProject.name = "Minesweeper"
rootProject.buildFileName = "build.gradle.kts"

include("annotations", "annotation-processor", "graphics-library", "minesweeper", "server")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            library("serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
            library("kotlin-logging", "io.github.microutils:kotlin-logging:3.0.4")
            library("slf4j-api", "org.slf4j:slf4j-api:2.0.6")

            version("logback", "1.4.5")
            library("logback-core", "ch.qos.logback", "logback-core").versionRef("logback")
            library("logback-classic", "ch.qos.logback", "logback-classic").versionRef("logback")

            library("auto-service", "com.google.auto.service:auto-service:1.0.1")
            library("kotlin-poet", "com.squareup:kotlinpoet:1.12.0")
        }
    }
}
