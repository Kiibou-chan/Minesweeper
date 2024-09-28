import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("kapt")

    application
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(libs.serialization)

    // Processing

    implementation(fileTree("../processing"))

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
}

kapt {
    arguments {

    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
