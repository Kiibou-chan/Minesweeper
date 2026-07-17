import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation(kotlin("reflect"))

    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.serialization)

    // Subproject Dependencies

    api(project(":annotations"))

    compileOnly(project(":annotation-processor"))
    kapt(project(":annotation-processor"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("space.kiibou.net.server.ServerKt")
}

kotlin {
    jvmToolchain(24)
}
