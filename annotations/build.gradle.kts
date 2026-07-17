import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.serialization)
    
    // Subproject Dependencies

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

kotlin {
    jvmToolchain(24)
}
