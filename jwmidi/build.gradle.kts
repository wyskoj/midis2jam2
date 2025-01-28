plugins {
    kotlin("jvm")
}

group = "org.wysko"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kmidi)
    implementation(libs.kotlinxCoroutines)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}