@file:Suppress("SpellCheckingInspection")

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    `java-library`
    // Creating shadow JARs
    id("com.github.johnrengelman.shadow") version "7.1.2"

    // Grabbing license dependencies
    id("com.github.hierynomus.license-report") version "0.16.1"

    // Kotlin
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.dokka") version "1.4.32"
    id("org.jetbrains.compose") version "1.0.1"

    java
    idea
    application
}

repositories {
    mavenCentral()
    maven(url = "https://maven.ej-technologies.com/repository")
    maven(url = "https://repo.spongepowered.org/repository/maven-public")
}

// Register main class
project.setProperty("mainClassName", "org.wysko.midis2jam2.MainKt")

// Configure Java version and build
tasks.compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

// Configure Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

application {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

tasks.processResources {
    dependsOn(tasks.getByName("downloadLicenses"))
    doFirst {
        /* Write build time and hash if present */
        val gitHash = System.getenv("GIT_HASH")
        val buildTime = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now())
        File(projectDir, "src/main/resources/build.txt").writeText("$buildTime${gitHash?.let { " $it" } ?: ""}")
        println("buildtime: $buildTime")

        /* Dependency report */
        copy {
            from("build/reports/license/dependency-license.json")
            into("src/main/resources")
        }

        /* Version */
        val midis2jam2Version: String by project
        File(projectDir, "src/main/resources/version.txt").writeText(midis2jam2Version)
    }
}

dependencies {
    // JMonkeyEngine
    implementation("org.jmonkeyengine:jme3-core:3.5.1-stable")
    implementation("org.jmonkeyengine:jme3-desktop:3.5.1-stable")
    implementation("org.jmonkeyengine:jme3-lwjgl:3.5.1-stable")
    implementation("org.jmonkeyengine:jme3-plugins:3.5.1-stable")
    implementation("org.jmonkeyengine:jme3-effects:3.5.1-stable")

    // JetBrains annotations
    implementation("org.jetbrains:annotations:23.0.0")

    // Unit testing
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // Theme
    implementation("com.formdev:flatlaf:2.1")

    // Installer
    implementation("com.install4j:install4j-runtime:8.0.7")

    // Noise library
    implementation("org.spongepowered:noise:2.0.0-SNAPSHOT")

    // Apache Commons CLI
    implementation("commons-cli:commons-cli:1.5.0")

    // Jetpack compose
    if (project.hasProperty("targetplatform")) {
        when (project.properties["targetplatform"]) {
            "windows" -> implementation(compose.desktop.windows_x64)
            "macos" -> implementation(compose.desktop.macos_x64)
            "linux" -> implementation(compose.desktop.linux_x64)
            else -> {
                println("Unknown target platform: ${project.properties["targetplatform"]}, reverting to default")
                implementation(compose.desktop.currentOs)
            }
        }
        println("used platform: ${project.properties["targetplatform"]}")
    } else {
        implementation(compose.desktop.currentOs)
    }

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // Logging

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha14")

}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("midis2jam2-${
        if (project.hasProperty("targetplatform")) {
            when (project.properties["targetplatform"]) {
                "windows" -> "windows"
                "macos" -> "macos"
                "linux" -> "linux"
                else -> {
                    println("Unknown target platform: ${project.properties["targetplatform"]}, reverting to default")
                    "current"
                }
            }
        } else {
            "current"
        }
    }-${project.properties["midis2jam2Version"]}.jar")
}

// Configure dependency licenses
downloadLicenses {
    includeProjectDependencies = true
    dependencyConfiguration = "compileClasspath"
}

// Configure Dokka
tasks.dokkaHtml.configure {
    moduleName.set("midis2jam2")
    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(true)
            reportUndocumented.set(true)
            suppressObviousFunctions.set(true)
        }
    }
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
          {
            "customStyleSheets": [
              "${rootDir.toString().replace('\\', '/')}/doc/logo-styles.css"
            ],
            "customAssets" : [
              "${rootDir.toString().replace('\\', '/')}/doc/midis2jam2.svg"
            ]
          }
          """.trimIndent()
        )
    )
}

tasks.test {
    useJUnitPlatform()
}