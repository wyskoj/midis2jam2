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

package org.wysko.midis2jam2.ui.settings.graphics

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.settings_graphics_anti_aliasing_description
import midis2jam2.app.generated.resources.settings_graphics_resolution_default_hint
import midis2jam2.app.generated.resources.settings_graphics_resolution_fullscreen
import midis2jam2.app.generated.resources.settings_graphics_shadows_description
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption

object GraphicsSettingsScreenModel : ScreenModel {
    @Composable
    fun formatResolution(
        isFullscreen: Boolean,
        isUseDefaultResolution: Boolean,
        resolutionWidth: Int,
        resolutionHeight: Int,
    ): String {
        if (isFullscreen) {
            return stringResource(Res.string.settings_graphics_resolution_fullscreen)
        }

        return when (isUseDefaultResolution) {
            true -> stringResource(Res.string.settings_graphics_resolution_default_hint)
            false -> "$resolutionWidth × $resolutionHeight"
        }
    }

    @Composable
    fun formatShadowsLabel(shadowsQualityOption: SettingsOption<ShadowsQuality>): String =
        "${stringResource(Res.string.settings_graphics_shadows_description)} ∙ ${shadowsQualityOption.title}"

    @Composable
    fun formatAntiAliasingLabel(antiAliasingQuality: SettingsOption<AntiAliasingQuality?>): String =
        "${stringResource(Res.string.settings_graphics_anti_aliasing_description)} ∙ ${antiAliasingQuality.title}"
}
