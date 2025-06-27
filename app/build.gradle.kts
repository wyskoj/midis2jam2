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

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

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
            implementation(libs.midiDriver)
            implementation(libs.appcompat)
            implementation(libs.fluidsynth)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            // -- FRONTEND LAYER -- //

            // Compose
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.compose.materialIcons)
            implementation(libs.compose.colorpicker)

            // DI and UI navigation
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.bundles.voyager)
            implementation(libs.multiplatformSettings)
            implementation(project.dependencies.platform(libs.koin.bom))

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Filekit
            implementation(libs.filekit)

            // AndroidX
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // -- BACKEND LAYER -- //

            // jMonkeyEngine
            implementation(libs.bundles.jme3)

            // kmidi + Gervill
            implementation(libs.kmidi)
            implementation(files("../libs/Gervill-0.2.31.jar"))

            // Misc.
            implementation(libs.logbackClassic)
            implementation(libs.noise)
            implementation(libs.kotlin.reflect)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.jme3.desktop)
            implementation(libs.jme3.lwjgl3)
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
        versionCode = 1
        versionName = "2.0.0-alpha-20250624"

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
        @Suppress("UnstableApiUsage")
        localeFilters += listOf("en-rUS", "fr")
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

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.wysko.midis2jam2"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register<Copy>("copyCommonResources") {
    from("../sharedAssets")
    into("src/commonMain/resources")
}

tasks.register<Copy>("copyAndroidResources") {
    from("../sharedAssets")
    into("src/androidMain/assets")
}

tasks.named("preBuild") {
    dependsOn("copyCommonResources", "copyAndroidResources")
}
