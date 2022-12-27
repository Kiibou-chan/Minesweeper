plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.serialization)

    // Subproject Dependencies

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
