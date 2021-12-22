val serializationVersion = "1.3.1"

plugins {
    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

repositories {
    jcenter()
    mavenCentral()
}

kotlin {
    jvm("minesweeper")
    jvm("guiLib")
    jvm("server")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }

        val serverMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
            }
        }

        val guiLibMain by getting {
            dependsOn(commonMain)

            // TODO (Svenja, 20/12/2021): Remove Dependency to Server Main
            dependsOn(serverMain)
            dependencies {
                implementation(fileTree("processing"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val minesweeperMain by getting {
            dependsOn(guiLibMain)
            dependsOn(serverMain)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}