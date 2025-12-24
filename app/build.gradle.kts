/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import com.github.jk1.license.render.TextReportRenderer
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.licenseReport)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

val appVersionName: String = "2.0.3"
val appVersionCode: Int = 9

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-Xexpect-actual-classes",
        )
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.jme3.androidNative)
            implementation(libs.koin.android)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonMain.dependencies {
            val composeBom = project.dependencies.platform(libs.androidx.compose.bom)
            implementation(composeBom)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // DI and UI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.bundles.voyager)
            implementation(libs.multiplatformSettings)
            implementation(project.dependencies.platform(libs.koin.bom))

            // Components
            implementation(libs.compose.colorpicker)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.yaml)

            // Filekit
            implementation(libs.filekit)

            // Reorderable
            implementation(libs.reorderable)

            // jMonkeyEngine
            implementation(libs.bundles.jme3)

            // kmidi
            implementation(libs.kmidi)

            // Kotlin
            implementation(libs.kotlin.reflect)

            // Misc.
            implementation(libs.logbackClassic)
            implementation(libs.noise)
        }

        desktopMain.dependencies {
            // Compose
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // jMonkeyEngine
            implementation(libs.jme3.desktop)
            implementation(libs.jme3.lwjgl3)

            // install4j integration
            implementation(libs.install4j.runtime)
        }
    }
}

android {
    namespace = "org.wysko.midis2jam2"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.wysko.midis2jam2"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "META-INF/**"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        dataBinding = true
    }
    androidResources {
        defaultConfig {
            resourceConfigurations.addAll(
                listOf(
                    "en-rUS",
                    "de",
                    "es",
                    "fi",
                    "fr",
                    "hi",
                    "it",
                    "ja",
                    "ko",
                    "no",
                    "pl",
                    "ru",
                    "th",
                    "tl",
                    "tr",
                    "uk",
                    "zh",
                )
            )
        }
    }
    packaging {
        jniLibs.pickFirsts.add("**/libc++_shared.so")
    }
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/CMakeLists.txt")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.wysko.midis2jam2.MainKt"

        buildTypes {
            release {
                proguard {
                    isEnabled.set(false)
                }
            }
        }

        nativeDistributions {
            packageName = "midis2jam2"
            packageVersion = appVersionName
        }
    }
}

licenseReport {
    renderers = arrayOf(TextReportRenderer())
}

abstract class GenerateBuildInfoXmlTask : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:OutputDirectory
    abstract val resourcesDir: DirectoryProperty

    @TaskAction
    fun generateXml() {
        resourcesDir.get().asFile.mkdirs()

        val buildTime = DateTimeFormatter
            .RFC_1123_DATE_TIME
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(Instant.now())

        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="build_version">${versionName.get()}</string>
                <string name="build_version_code">${versionCode.get()}</string>
                <string name="build_timestamp">$buildTime</string>
            </resources>
        """.trimIndent()

        File(resourcesDir.get().asFile, "build.xml").writeText(xmlContent)

        println("Generated build info XML with version ${versionName.get()} and timestamp $buildTime")
    }
}

val generateBuildInfoXml: TaskProvider<GenerateBuildInfoXmlTask> =
    tasks.register<GenerateBuildInfoXmlTask>("generateBuildInfoXml") {
        description = "Generates build.xml with version and timestamp information"
        versionName.set(appVersionName)
        versionCode.set(appVersionCode)
        resourcesDir.set(file("src/commonMain/composeResources/values"))
    }

val copyLicenseReport: TaskProvider<Copy> = tasks.register<Copy>("copyLicenseReport") {
    dependsOn(tasks.named("generateLicenseReport"))
    from(projectDir.resolve("build/reports/dependency-license/THIRD-PARTY-NOTICES.txt"))
    into(projectDir.resolve("src/commonMain/composeResources/files"))
}

val copyCommonAssets: TaskProvider<Copy> = tasks.register<Copy>("copyCommonAssets") {
    from(projectDir.parentFile.resolve("sharedAssets"))
    into(projectDir.resolve("src/commonMain/resources"))
}

val copyAndroidAssets: TaskProvider<Copy> = tasks.register<Copy>("copyAndroidAssets") {
    from(projectDir.parentFile.resolve("sharedAssets"))
    into(projectDir.resolve("src/androidMain/assets"))
}
dependencies {
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.named("convertXmlValueResourcesForCommonMain").configure {
    dependsOn(generateBuildInfoXml, copyLicenseReport)
}

tasks.named("copyNonXmlValueResourcesForCommonMain").configure {
    dependsOn(generateBuildInfoXml, copyLicenseReport)
}

afterEvaluate {
    // Make all Android tasks that might use assets depend on copyAndroidAssets
    tasks.matching { task ->
        val relevant = task.name.contains("Assets") || task.name.contains("Lint") || task.name.contains("Resources")
        task.name != "copyAndroidAssets" && relevant && task.name.contains("Android", ignoreCase = true)
    }.configureEach {
        dependsOn(copyAndroidAssets)
    }

    // Make all resource processing tasks depend on copyCommonAssets
    tasks.matching { task ->
        task.name != "copyCommonAssets" && (task.name.contains("ProcessResources") || task.name.contains("Resources"))
    }.configureEach {
        dependsOn(copyCommonAssets)
    }
}
