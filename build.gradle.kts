import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("kapt") version "2.2.21"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(24)
}
