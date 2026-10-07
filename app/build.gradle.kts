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
import org.jetbrains.compose.reload.gradle.ComposeHotRun
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
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.licenseReport)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("org.wysko.midis2jam2.MainKt")
}

val appVersionName: String = "2.1.2"
val appVersionCode: Int = 12

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
            implementation(libs.bundles.filekit)

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

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.yaml)
                implementation(libs.jme3.desktop)
                implementation(libs.kotlin.reflect)
            }
        }

        desktopMain.dependencies {
            // Compose
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // jMonkeyEngine
            implementation(libs.jme3.desktop)

            val os = System.getProperty("os.name").lowercase()
            if (os.contains("mac")) {
                implementation(libs.jme3.lwjgl)
            } else {
                implementation(libs.jme3.lwjgl3)
            }

            // install4j integration
            implementation(libs.install4j.runtime)

            // Video recording: javacv's FFmpegFrameRecorder only needs javacpp and ffmpeg, so its other
            // transitive dependencies (OpenCV, Tesseract, camera SDKs, ...) are left out.
            implementation(libs.javacv.get().toString()) { isTransitive = false }
            implementation(libs.javacpp)
            implementation(libs.bytedeco.ffmpeg)
            javacppPlatforms().forEach { platform ->
                implementation("${libs.javacpp.get()}:$platform")
                implementation("${libs.bytedeco.ffmpeg.get()}:$platform-gpl")
            }
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
                arguments.addAll(listOf("-DANDROID_STL=c++_shared", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"))
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
                    "vi",
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

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
    // The suite must run with no display, no window and no audio device attached.
    systemProperty("java.awt.headless", "true")
    // Recording renders audio offline with Gervill, whose API for that isn't exported (see OfflineSynthesizer).
    jvmArgs("--add-exports=java.desktop/com.sun.media.sound=ALL-UNNAMED")
    // SpecCoverageTest scans the compiled test classes for @Spec citations.
    val compiledTestClasses = testClassesDirs
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            val dirs = compiledTestClasses.joinToString(File.pathSeparator) { it.absolutePath }
            listOf("-Dmidis2jam2.testClassesDirs=$dirs")
        }
    )
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// Shot Lab: a tool for rating the cinematic camera's shots, one at a time. It isn't part of the app; it lives with
// the desktop tests (tools/shotlab). Run with ./gradlew :app:shotLab, optionally -PshotLab="<files or folders;...>".
tasks.register<JavaExec>("shotLab") {
    group = "tools"
    description = "Plays the cinematic camera's shots one at a time for rating."
    val testCompilation = kotlin.jvm("desktop").compilations.getByName("test")
    classpath = files(testCompilation.output.allOutputs, testCompilation.runtimeDependencyFiles)
    mainClass.set("org.wysko.midis2jam2.tools.shotlab.ShotLabKt")
    workingDir = rootDir
    jvmArgs("--add-exports=java.desktop/com.sun.media.sound=ALL-UNNAMED")
    providers.gradleProperty("shotLab").orNull?.let { args(it) }
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

// Assets (see docs/ASSETS.md). The model sources in sharedAssets/models are converted to .j3o by :asset-tools and
// shipped alongside the rest of sharedAssets; the same tool generates the typed catalog code refers to them by.
val sharedAssetsDir: File = rootDir.resolve("sharedAssets")

val assetTools: Configuration by configurations.creating {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    assetTools(project(":asset-tools"))
}

val generateAssetCatalog: TaskProvider<JavaExec> = tasks.register<JavaExec>("generateAssetCatalog") {
    group = "assets"
    description = "Generates the typed asset catalog (Models, Materials, Textures) from sharedAssets."
    val outDir = layout.buildDirectory.dir("generated/assetCatalog/kotlin")
    classpath = assetTools
    mainClass.set("org.wysko.midis2jam2.assettools.MainKt")
    inputs.dir(sharedAssetsDir.resolve("models")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(sharedAssetsDir.resolve("Assets/Materials")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(sharedAssetsDir.resolve("Assets/Textures")).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(outDir)
    args("catalog", sharedAssetsDir.absolutePath, outDir.get().asFile.absolutePath)
}

val convertModels: TaskProvider<JavaExec> = tasks.register<JavaExec>("convertModels") {
    group = "assets"
    description = "Converts the OBJ sources in sharedAssets/models to .j3o, with their library materials."
    val outDir = layout.buildDirectory.dir("generated/jmeAssets")
    classpath = assetTools
    mainClass.set("org.wysko.midis2jam2.assettools.MainKt")
    inputs.dir(sharedAssetsDir.resolve("models")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        fileTree(sharedAssetsDir.resolve("Assets")) {
            include("Materials/**", "MatDefs/**", "Shaders/**", "**/*.bmp", "**/*.png")
        }
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(outDir)
    systemProperty("java.awt.headless", "true")
    args("convert", sharedAssetsDir.absolutePath, outDir.get().asFile.absolutePath)
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateAssetCatalog)
}

tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(generateAssetCatalog)
}

// Sync rather than Copy, so that a moved or deleted asset disappears from the copy too, instead of lingering there
// and hiding a reference that was not updated. Both destinations are gitignored and hold nothing else.
fun Sync.shippedAssets() {
    from(sharedAssetsDir) { exclude("models/**") }
    from(convertModels)
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

val copyCommonAssets: TaskProvider<Sync> = tasks.register<Sync>("copyCommonAssets") {
    shippedAssets()
    into(projectDir.resolve("src/commonMain/resources"))
}

val copyAndroidAssets: TaskProvider<Sync> = tasks.register<Sync>("copyAndroidAssets") {
    shippedAssets()
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

/**
 * The JavaCPP platform classifiers whose natives are bundled into this build. Releases are built per OS, so only
 * the host's natives are included; macOS gets both architectures so one jar runs on Intel and Apple silicon.
 */
fun javacppPlatforms(): List<String> {
    val os = System.getProperty("os.name").lowercase()
    val arch = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "arm64"
        else -> "x86_64"
    }
    return when {
        os.contains("mac") -> listOf("macosx-x86_64", "macosx-arm64")
        os.contains("win") -> listOf("windows-$arch")
        else -> listOf("linux-$arch")
    }
}
