plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application

    alias(libs.plugins.compose)
    alias(libs.plugins.compose.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))

    // Compose Multiplatform
    implementation(compose.desktop.windows_x64)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(libs.filekit)
    implementation(libs.reorderable)
    implementation(libs.multiplatformSettings)

    implementation(libs.bundles.koin)
    implementation(libs.bundles.voyager)
    implementation(libs.bundles.kotlinxEcosystem)
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.wysko.midis2jam2.MainKt"
}

compose.resources {
    customDirectory(
        sourceSetName = "main",
        directoryProvider = provider { layout.projectDirectory.dir("src/main/resources/composeResources") },
    )
}
kotlin {
    sourceSets {
        getByName("main") {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }
}