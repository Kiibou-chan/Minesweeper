import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("org.openjfx.javafxplugin") version "0.1.0"

    application
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.serialization)

    // Processing

    implementation(libs.processing)

    // reactive kotlin

    implementation("space.kiibou.reactive-kotlin:REKotlin:0.1.1")

    // Subproject Dependencies

    implementation(project(":graphics-library"))
    implementation(project(":server"))

    compileOnly(project(":annotation-processor"))
    kapt(project(":annotation-processor"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation(testFixtures(project(":graphics-library")))
}

javafx {
    modules("javafx.controls")
}

application {
    mainClass.set("space.kiibou.MinesweeperMain")
}

kapt {
    arguments {

    }
}

kotlin {
    jvmToolchain(24)
}
