
plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    kotlin("jvm")

    // Apply the application plugin to add support for building a CLI application in Java.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    // Processing

    implementation(fileTree("../processing"))

    // Subproject Dependencies

    // TODO (Svenja, 2022/12/27): Remove dependency on server
    implementation(project(":server"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
