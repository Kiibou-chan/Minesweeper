plugins {
    kotlin("jvm")
    kotlin("kapt")

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.poet)

    implementation(libs.auto.service)
    kapt(libs.auto.service)

    // Subproject Dependencies

    implementation(project(":server"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
