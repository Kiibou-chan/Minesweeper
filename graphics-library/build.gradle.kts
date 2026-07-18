plugins {
    kotlin("jvm")
    `java-library`
    `java-test-fixtures`
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    // Processing

    implementation(libs.processing)

    // reactive kotlin

    implementation("space.kiibou.reactive-kotlin:REKotlin:0.1.1")

    // Subproject Dependencies

    // TODO (Svenja, 2022/12/27): Remove dependency on server
    implementation(project(":server"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))

    // Test fixtures (GuiRobot) drive Processing events directly.

    testFixturesImplementation(libs.processing)
}

javafx {
    modules("javafx.controls")
}

kotlin {
    jvmToolchain(24)
}

tasks.withType<Test>().all {
    jvmArgs(
        "--add-exports=jogl.all/com.jogamp.opengl.glu=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.newt=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.newt.opengl=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.newt.event=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.opengl.util=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.newt.util=ALL-UNNAMED",
        "--add-exports=jogl.all/com.jogamp.nativewindow.util=ALL-UNNAMED",
    )
}
