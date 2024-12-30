import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    application
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.licenseReport)
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.versions)
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
    implementation(libs.bundles.jme3)

    // Compose Multiplatform
    if (project.ext.has("targetplatform")) {
        when (project.ext["targetplatform"]) {
            "windows" -> {
                implementation(compose.desktop.windows_x64)
                implementation(libs.jme3.lwjgl3)
            }

            "macos_x64" -> {
                implementation(compose.desktop.macos_x64)
                implementation(libs.jme3.lwjgl)
            }

            "macos_arm64" -> {
                implementation(compose.desktop.macos_arm64)
                implementation(libs.jme3.lwjgl)
            }

            "linux_x64" -> {
                implementation(compose.desktop.linux_x64)
                implementation(libs.jme3.lwjgl3)
            }

            "linux_arm64" -> {
                implementation(compose.desktop.linux_arm64)
                implementation(libs.jme3.lwjgl3)
            }

            else -> {
                println("Unknown target platform: ${project.properties["targetplatform"]}, reverting to default")
                implementation(compose.desktop.currentOs)
                implementation(libs.jme3.lwjgl3)
            }
        }
        println("used platform: ${project.properties["targetplatform"]}")
    } else {
        println("used platform: current")
        implementation(compose.desktop.currentOs)
        implementation(libs.jme3.lwjgl3)
    }
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.mpfilepicker)
    implementation(libs.install4j.runtime)
    implementation(libs.noise)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.logback.classic)
    implementation(libs.kmidi)
    implementation(files("libs/Gervill-0.2.31.jar", "libs/jme-ttf-2.2.2.jar"))
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

compose.resources {
    customDirectory(
        sourceSetName = "main",
        directoryProvider = provider { layout.projectDirectory.dir("src/main/resources/composeResources") },
    )
}