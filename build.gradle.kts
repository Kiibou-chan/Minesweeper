import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    kotlin("kapt") version "2.0.20"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}
