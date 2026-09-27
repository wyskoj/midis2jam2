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

package org.wysko.midis2jam2.testing

import java.io.File

/**
 * Locations in the source tree that integrity tests read.
 *
 * Tests inspect the authored sources under `sharedAssets/` and `app/src/`, not the copies
 * Gradle makes into the resource directories, so that a stale copy can never mask a problem.
 */
object ProjectPaths {

    /** The repository root, found by walking up from the test's working directory. */
    val repositoryRoot: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "sharedAssets").isDirectory && File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repository root from ${File("").absolutePath}")
    }

    val sharedAssets: File get() = File(repositoryRoot, "sharedAssets")
    val sourceRoot: File get() = File(repositoryRoot, "app/src")
    val composeResources: File get() = File(repositoryRoot, "app/src/commonMain/composeResources")
    val appBuildScript: File get() = File(repositoryRoot, "app/build.gradle.kts")

    /** Every Kotlin source file in the project, across all source sets. */
    val kotlinSources: List<File> by lazy {
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.contains("${File.separator}desktopTest${File.separator}") }
            .filterNot { it.path.contains("${File.separator}commonTest${File.separator}") }
            .toList()
    }

    /** The concatenated text of every Kotlin source file, for "is this referenced anywhere" checks. */
    val allKotlinSourceText: String by lazy {
        kotlinSources.joinToString("\n") { it.readText() }
    }
}
