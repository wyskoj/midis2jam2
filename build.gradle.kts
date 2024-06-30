@file:Suppress("SpellCheckingInspection")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.6.11"
    id("io.gitlab.arturbosch.detekt") version ("1.23.3")

    java
    idea
    application
}

repositories {
    mavenCentral()
    maven(url = "https://maven.ej-technologies.com/repository")
    maven(url = "https://repo.spongepowered.org/repository/maven-public")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

// Register main class
project.setProperty("mainClassName", "org.wysko.midis2jam2.MainKt")

// Configure Java version and build
tasks.compileJava {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

// Configure Kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xcontext-receivers")
    }
}

application {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

tasks.processResources {
    dependsOn(tasks.getByName("downloadLicenses"))
    doFirst {
        // Write build time and hash if present
        val gitHash = System.getenv("GIT_HASH")
        val buildTime = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now())
        File(projectDir, "src/main/resources/build.txt").writeText("$buildTime${gitHash?.let { " $it" } ?: ""}")
        println("buildtime: $buildTime")

        // Dependency report
        copy {
            from("build/reports/license/dependency-license.json")
            into("src/main/resources")
        }

        // Version
        val midis2jam2Version: String by project
        File(projectDir, "src/main/resources/version.txt").writeText(midis2jam2Version)
    }
}

dependencies {
    // JMonkeyEngine
    val jmeVersion = "3.6.1-stable"

    implementation("org.jmonkeyengine:jme3-core:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-desktop:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-plugins:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-effects:$jmeVersion")

    // JetBrains annotations
    implementation("org.jetbrains:annotations:23.0.0")

    // Unit testing
    testImplementation(platform("org.junit:junit-bom:5.7.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")

    // Installer
    implementation("com.install4j:install4j-runtime:8.0.7")

    // Noise library
    implementation("org.spongepowered:noise:2.0.0-SNAPSHOT")

    // Apache Commons CLI
    implementation("commons-cli:commons-cli:1.5.0")

    // Jetpack compose
    if (project.hasProperty("targetplatform")) {
        when (project.properties["targetplatform"]) {
            "windows" -> {
                implementation(compose.desktop.windows_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
            }

            "macos" -> {
                implementation(compose.desktop.macos_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl:$jmeVersion")
            }

            "linux" -> {
                implementation(compose.desktop.linux_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
            }

            else -> {
                println("Unknown target platform: ${project.properties["targetplatform"]}, reverting to default")
                implementation(compose.desktop.currentOs)
                implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
            }
        }
        println("used platform: ${project.properties["targetplatform"]}")
    } else {
        implementation(compose.desktop.currentOs)
        implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
    }
    implementation(compose.material3)
    implementation("com.darkrockstudios:mpfilepicker:2.0.2")

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation(files("libs/Gervill-0.2.31.jar", "libs/jme-ttf-2.2.2.jar"))
    implementation("org.nutz:nutz-plugins-sfntly:1.r.60.r3")

    implementation("org.wysko:kmidi:0.0.4")
    implementation("io.humble:humble-video-all:0.3.0")
}

tasks.withType<ShadowJar> {
    archiveFileName.set(
        "midis2jam2-${
            if (project.hasProperty("targetplatform")) {
                when (project.properties["targetplatform"]) {
                    "windows" -> "windows"
                    "macos" -> "macos"
                    "linux" -> "linux"
                    else -> {
                        println(
                            "Unknown target platform: ${project.properties["targetplatform"]}, reverting to default"
                        )
                        "current"
                    }
                }
            } else {
                "current"
            }
        }-${project.properties["midis2jam2Version"]}.jar",
    )
}

// Configure dependency licenses
downloadLicenses {
    includeProjectDependencies = true
    dependencyConfiguration = "compileClasspath"
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom(files("detekt-config.yml"))
}
