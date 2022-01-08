@file:Suppress("SpellCheckingInspection")

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("org.panteleyev.jpackageplugin") version "1.3.0"
    id("com.github.hierynomus.license-report") version "0.15.0"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.dokka") version "1.4.32"
    id("org.jetbrains.compose") version "1.0.1"
    id("org.openjfx.javafxplugin") version "0.0.10"
    java
    idea
    application
}

project.setProperty("mainClassName", "org.wysko.midis2jam2.MainKt")

javafx {
    version = "11"
    modules = listOf("javafx.controls", "javafx.swing")
}

tasks.build {
    dependsOn("downloadLicenses")
    doFirst {
        val gitHash = System.getenv("GIT_HASH")
        val buildTime = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now())
        File("src/main/resources/build.txt").writeText("$buildTime${gitHash?.let { " $it" } ?: ""}")

        println("buildtime: $buildTime")

        copy {
            from("build/reports/license/dependency-license.json")
            into("src/main/resources")
        }
    }
}

tasks.compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

repositories {
    mavenCentral()
    maven(url = "https://maven.ej-technologies.com/repository")
    maven(url = "https://repo.spongepowered.org/repository/maven-public")
}

application {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

tasks.shadowJar {
    doFirst {
        File(projectDir, "src/main/resources/version.txt").writeText(archiveVersion.get())
    }
}

dependencies {
    // JMonkeyEngine
    implementation("org.jmonkeyengine:jme3-core:3.4.1-stable")
    implementation("org.jmonkeyengine:jme3-desktop:3.4.1-stable")
    implementation("org.jmonkeyengine:jme3-lwjgl:3.4.1-stable")
    implementation("org.jmonkeyengine:jme3-plugins:3.4.1-stable")

    // JetBrains annotations
    implementation("org.jetbrains:annotations:22.0.0")

    // Utility
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.8.9")

    // Unit testing
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // Theme
    implementation("com.formdev:flatlaf:1.6.5")

    // Installer
    implementation("com.install4j:install4j-runtime:8.0.7")

    // Noise library
    implementation("org.spongepowered:noise:2.0.0-SNAPSHOT")

    // Dokka math
    implementation("org.jetbrains.dokka:mathjax-plugin:1.6.0")

    // Apache Commons CLI
    implementation("commons-cli:commons-cli:1.5.0")

    // Jetpack compose
    implementation(compose.desktop.currentOs)

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

downloadLicenses {
    includeProjectDependencies = true
    dependencyConfiguration = "compileClasspath"
}

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

