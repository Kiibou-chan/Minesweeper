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

// Windowed GUI e2e tests (space.kiibou.e2e) skip themselves unless this flag is passed:
//   gradle :minesweeper:test -Dgui.e2e=true    (under xvfb-run on headless machines)

// JOGL cannot find its native jars in the Gradle cache (they are not siblings of the
// base jars as in an installDist lib dir), so extract the .so files for the test and run JVMs.
val extractJoglNatives by tasks.registering(Sync::class) {
    from(configurations.runtimeClasspath.map { classpath ->
        classpath.filter { it.name.contains("natives-linux-amd64") }.map { zipTree(it) }
    })
    include("**/*.so")
    eachFile { path = name }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("jogl-natives"))
}

tasks.named<JavaExec>("run") {
    dependsOn(extractJoglNatives)
    systemProperty("jogamp.gluegen.UseTempJarCache", "false")
    systemProperty("java.library.path", layout.buildDirectory.dir("jogl-natives").get().asFile.absolutePath)
}

tasks.withType<Test>().configureEach {
    val guiE2e = System.getProperty("gui.e2e", "false")
    systemProperty("gui.e2e", guiE2e)
    if (guiE2e == "true") {
        dependsOn(extractJoglNatives)
        // Gradle test workers are headless by default; JOGL's AWT bridge needs a display.
        systemProperty("java.awt.headless", "false")
        systemProperty("jogamp.gluegen.UseTempJarCache", "false")
        systemProperty("java.library.path", layout.buildDirectory.dir("jogl-natives").get().asFile.absolutePath)
    }
}
