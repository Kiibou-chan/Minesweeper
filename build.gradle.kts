val serializationVersion = "1.4.1"

plugins {
    kotlin("multiplatform") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

repositories {
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
                implementation("io.github.microutils:kotlin-logging:1.7.4")

            }
        }

        val serverMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("reflect"))

                implementation("org.slf4j:slf4j-api:2.0.6")

                runtimeOnly("ch.qos.logback:logback-core:1.4.5")
                runtimeOnly("ch.qos.logback:logback-classic:1.4.5")
            }
        }

        val guiLibMain by getting {
            dependsOn(commonMain)

            // TODO (Svenja, 20/12/2021): Remove Dependency to Server Main
            dependsOn(serverMain)

            dependencies {
                implementation(fileTree("processing"))

                implementation("org.slf4j:slf4j-api:2.0.6")

                runtimeOnly("ch.qos.logback:logback-core:1.4.5")
                runtimeOnly("ch.qos.logback:logback-classic:1.4.5")
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