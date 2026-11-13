pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // ModDevGradle + NeoForge plugin marker artifacts
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    // Lets Gradle fetch the JDK 21 / 25 toolchains itself, so CI needs only one JDK to
    // launch the daemon and the repo builds anywhere without a local JDK 25 install.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mmcr-nekojs"

include(":mc1211")
include(":mc2612")
project(":mc1211").projectDir = file("versions/1.21.1")
project(":mc2612").projectDir = file("versions/26.1.2")
