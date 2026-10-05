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

package org.wysko.midis2jam2.domain

import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER
import java.io.File

/**
 * What is wrong with the cube map, by side.
 *
 * Both lists hold positions in [AppSettings.BackgroundSettings.cubeMapTextures].
 *
 * @property unassigned The sides that have no image chosen.
 * @property missing The sides whose chosen file is not in the backgrounds folder.
 */
data class CubeMapProblems(val unassigned: List<Int>, val missing: List<Int>) {
    val isEmpty: Boolean get() = unassigned.isEmpty() && missing.isEmpty()
}

/**
 * Finds the sides of the cube map that cannot be used, resolving file names against [folder].
 *
 * There are never problems unless the cube map is the chosen background.
 */
fun cubeMapProblems(
    bg: AppSettings.BackgroundSettings,
    folder: File = BACKGROUND_IMAGES_FOLDER,
): CubeMapProblems {
    if (bg.type != BackgroundType.CubeMap) return CubeMapProblems(emptyList(), emptyList())
    val textures = bg.cubeMapTextures
    return CubeMapProblems(
        unassigned = textures.indices.filter { textures[it].isBlank() },
        missing = textures.indices.filter { textures[it].isNotBlank() && !File(folder, textures[it]).exists() },
    )
}

/**
 * Computes the [BackgroundWarning] for the given [BackgroundSettings], resolving
 * file paths against [BACKGROUND_IMAGES_FOLDER].
 *
 * Returns `null` when there is no misconfiguration.
 */
fun computeBackgroundWarning(bg: AppSettings.BackgroundSettings): BackgroundWarning? = with(cubeMapProblems(bg)) {
    when {
        unassigned.isNotEmpty() -> BackgroundWarning.UNASSIGNED
        missing.isNotEmpty() -> BackgroundWarning.MISSING
        else -> null
    }
}
