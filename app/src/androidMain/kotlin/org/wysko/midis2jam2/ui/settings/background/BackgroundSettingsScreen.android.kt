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

package org.wysko.midis2jam2.ui.settings.background

import androidx.compose.runtime.Composable
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.settings_background_type_color
import midis2jam2.app.generated.resources.settings_background_type_color_description
import midis2jam2.app.generated.resources.settings_background_type_default
import midis2jam2.app.generated.resources.settings_background_type_default_description
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.settings.background.BackgroundSettingsScreen.BackgroundTypeIcon

internal actual val backgroundTypeOptions: List<SettingsOption<AppSettings.BackgroundSettings.BackgroundType>>
    @Composable
    get() = listOf(
        SettingsOption(
            value = AppSettings.BackgroundSettings.BackgroundType.Default,
            title = stringResource(Res.string.settings_background_type_default),
            label = stringResource(Res.string.settings_background_type_default_description),
            icon = { BackgroundTypeIcon(AppSettings.BackgroundSettings.BackgroundType.Default) }
        ),
        SettingsOption(
            value = AppSettings.BackgroundSettings.BackgroundType.Color,
            title = stringResource(Res.string.settings_background_type_color),
            label = stringResource(Res.string.settings_background_type_color_description),
            icon = { BackgroundTypeIcon(AppSettings.BackgroundSettings.BackgroundType.Color) }
        ),
    )
