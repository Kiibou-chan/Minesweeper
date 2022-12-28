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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
