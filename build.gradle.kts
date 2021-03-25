val serializationVersion = "1.1.0"

plugins {
    kotlin("multiplatform") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
}

repositories {
    jcenter()
    mavenCentral()
}

kotlin {
    jvm("minesweeper")
    jvm("guiLib")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }

        val guiLibMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(fileTree("processing"))
                implementation("com.fasterxml.jackson.core:jackson-core:2.11.3")
                implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
            }
        }

        val minesweeperMain by getting {
            dependsOn(guiLibMain)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}