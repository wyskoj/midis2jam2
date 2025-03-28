plugins {
    kotlin("jvm")
    kotlin("plugin.serialization").version(libs.versions.kotlin)
}

group = "org.wysko"
version = "unspecified"

repositories {
    mavenCentral()
    maven(url = "https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(project(":jmeKtDsl"))
    implementation(project(":jwmidi"))
    implementation(project(":utils"))

    implementation(kotlin("reflect"))

    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.kaml)
    implementation(libs.multiplatformSettings)
    implementation(libs.bundles.logging)

    // jMonkeyEngine
    implementation(libs.bundles.jme3)
    implementation(libs.jme3.lwjgl3)
    implementation(libs.noise)

    implementation(libs.kmidi)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinxCoroutines)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
