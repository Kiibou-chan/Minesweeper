plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.serialization)

    // Processing

    implementation(fileTree("../processing"))

    // Subproject Dependencies

    implementation(project(":graphics-library"))
    implementation(project(":server"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
