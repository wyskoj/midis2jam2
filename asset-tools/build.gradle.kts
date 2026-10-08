/*
 * Copyright (C) 2026 Jacob Wysko
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

// Build-time asset tooling: converts the OBJ sources under sharedAssets/models into .j3o files and generates
// the typed asset catalog that :app compiles against. It is run by :app's convertModels and
// generateAssetCatalog tasks, never shipped. See docs/ASSETS.md.

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // The same jME version as :app, so the .j3o format always matches the engine that reads it.
    implementation(libs.jme3.core)
    implementation(libs.jme3.desktop) // image loaders for the textures that materials pull in
    implementation(libs.kotlinx.serialization.yaml)
    implementation(libs.jme3.plugins) // the glTF loader, for the .glb model sources
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}
