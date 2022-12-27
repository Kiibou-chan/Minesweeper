plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("kapt")

    application
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

    compileOnly(project(":annotation-processor"))
    kapt(project(":annotation-processor"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
