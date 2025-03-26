plugins {
    kotlin("jvm")
    kotlin("plugin.serialization").version(libs.versions.kotlin)
}

group = "org.wysko"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.jme3)
    implementation(libs.bundles.kotlinxEcosystem)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
