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
 * Computes the [BackgroundWarning] for the given [BackgroundSettings], resolving
 * file paths against [BACKGROUND_IMAGES_FOLDER].
 *
 * Returns `null` when there is no misconfiguration.
 */
fun computeBackgroundWarning(bg: AppSettings.BackgroundSettings): BackgroundWarning? = when {
    bg.type != BackgroundType.CubeMap -> null
    bg.cubeMapTextures.any { it.isBlank() } -> BackgroundWarning.UNASSIGNED
    bg.cubeMapTextures.any { it.isNotBlank() && !File(BACKGROUND_IMAGES_FOLDER, it).exists() } -> BackgroundWarning.MISSING
    else -> null
}
