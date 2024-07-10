import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    application
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.6.11"
    id("io.gitlab.arturbosch.detekt") version ("1.23.6")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.hierynomus.license-report") version "0.16.1"
    id("com.autonomousapps.dependency-analysis") version "1.32.0"
    id("com.github.ben-manes.versions") version "0.51.0"
}

repositories {
    mavenCentral()
    maven(url = "https://maven.ej-technologies.com/repository")
    maven(url = "https://repo.spongepowered.org/repository/maven-public")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

application {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xcontext-receivers")
    }
}

tasks.processResources {
    dependsOn(tasks.getByName("downloadLicenses"))
    doFirst {
        val gitHash = System.getenv("GIT_HASH")
        val buildTime = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(Instant.now())
        File(projectDir, "src/main/resources/build.txt").writeText("$buildTime${gitHash?.let { " $it" } ?: ""}")
        println("Build time: $buildTime")

        copy {
            from("build/reports/license/dependency-license.json")
            into("src/main/resources")
        }

        val midis2jam2Version: String by project
        File(projectDir, "src/main/resources/version.txt").writeText(midis2jam2Version)
    }
}

dependencies {
    // JMonkeyEngine
    val jmeVersion = "3.6.1-stable"
    implementation("org.jmonkeyengine:jme3-core:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-desktop:$jmeVersion")
    implementation("org.jmonkeyengine:jme3-effects:$jmeVersion")

    // Compose Multiplatform
    if (project.ext.has("targetplatform")) {
        when (project.ext["targetplatform"]) {
            "windows" -> {
                implementation(compose.desktop.windows_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
            }

            "macos_x64" -> {
                implementation(compose.desktop.macos_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl:$jmeVersion")
            }

            "macos_arm64" -> {
                implementation(compose.desktop.macos_arm64)
                implementation("org.jmonkeyengine:jme3-lwjgl:$jmeVersion")
            }

            "linux_x64" -> {
                implementation(compose.desktop.linux_x64)
                implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
            }

            "linux_arm64" -> {
                implementation(compose.desktop.linux_arm64)
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
        println("used platform: current")
        implementation(compose.desktop.currentOs)
        implementation("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")
    }
    implementation(compose.material3)
    implementation("com.darkrockstudios:mpfilepicker:2.0.2")

    // Misc.
    implementation("com.install4j:install4j-runtime:10.0.8")
    implementation("org.spongepowered:noise:2.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation(files("libs/Gervill-0.2.31.jar", "libs/jme-ttf-2.2.2.jar"))
    implementation("org.wysko:kmidi:0.0.6")
}

tasks.withType<ShadowJar> {
    archiveFileName.set(
        "midis2jam2-${
            if (project.hasProperty("targetplatform")) {
                when (project.properties["targetplatform"]) {
                    "windows" -> "windows"
                    "macos_x64" -> "macos_x64"
                    "macos_arm64" -> "macos_arm64"
                    "linux_x64" -> "linux_x64"
                    "linux_arm64" -> "linux_arm64"
                    else -> {
                        println(
                            "Unknown target platform: ${project.properties["targetplatform"]}, reverting to default",
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

detekt { config.setFrom(files("detekt-config.yml")) }

downloadLicenses {
    includeProjectDependencies = true
    dependencyConfiguration = "compileClasspath"
}
