plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.poet)

    implementation(libs.serialization)

    implementation(libs.auto.service)
    kapt(libs.auto.service)

    // Subproject Dependencies

    implementation(project(":annotations"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
