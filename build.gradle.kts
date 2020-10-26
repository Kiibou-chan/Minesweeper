val serializationVersion = "1.0.0"

plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
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
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }

        val guiLibMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.processing:core:3.3.7")
                implementation("com.fasterxml.jackson.core:jackson-core:2.11.3")
                implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
            }
        }

        val minesweeperMain by getting {
            dependsOn(guiLibMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
    }
}