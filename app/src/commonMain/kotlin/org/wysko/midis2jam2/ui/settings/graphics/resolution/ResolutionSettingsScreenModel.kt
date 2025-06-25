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

package org.wysko.midis2jam2.ui.settings.graphics.resolution

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.settings_graphics_resolution_default_hint
import org.jetbrains.compose.resources.stringResource

object ResolutionSettingsScreenModel : ScreenModel {
    @Composable
    fun formatResolution(isDefault: Boolean, resolutionWidth: Int, resolutionHeight: Int): String = when (isDefault) {
        true -> stringResource(Res.string.settings_graphics_resolution_default_hint)
        false -> "$resolutionWidth × $resolutionHeight"
    }

    fun validateResolution(resolutionWidth: String, resolutionHeight: String): Boolean =
        resolutionWidth.toIntOrNull()?.let { it > 0 } == true && resolutionHeight.toIntOrNull()?.let { it > 0 } == true
}
