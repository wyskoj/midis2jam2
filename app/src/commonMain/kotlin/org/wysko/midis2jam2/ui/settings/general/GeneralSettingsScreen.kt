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

package org.wysko.midis2jam2.ui.settings.general

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.dark_mode
import midis2jam2.app.generated.resources.light_mode
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_theme
import midis2jam2.app.generated.resources.settings_general_theme_dark
import midis2jam2.app.generated.resources.settings_general_theme_light
import midis2jam2.app.generated.resources.settings_general_theme_system
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
internal expect fun LocaleSettingsCard(
    settings: AppSettings,
    model: SettingsModel,
)

object GeneralSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val model = koinScreenModel<SettingsModel>()
        val settings = model.appSettings.collectAsState()

        SettingsScaffold(title = { Text(stringResource(Res.string.settings_general)) }) {
            val themeOptions = listOf(
                SettingsOption(
                    value = AppTheme.LIGHT,
                    title = stringResource(Res.string.settings_general_theme_light),
                    icon = { Icon(painterResource(Res.drawable.light_mode), "") }
                ),
                SettingsOption(
                    value = AppTheme.DARK,
                    title = stringResource(Res.string.settings_general_theme_dark),
                    icon = { Icon(painterResource(Res.drawable.dark_mode), "") }
                ),
                SettingsOption(
                    value = AppTheme.SYSTEM_DEFAULT,
                    title = stringResource(Res.string.settings_general_theme_system),
                    icon = { SystemDefaultIcon() }
                )
            )

            SettingsOptionsCard(
                title = { Text(stringResource(Res.string.settings_general_theme)) },
                icon = { themeOptions.find { it.value == settings.value.generalSettings.theme }?.icon?.invoke() },
                options = themeOptions,
                selectedOption = settings.value.generalSettings.theme,
                onOptionSelected = model::setAppTheme
            )
            LocaleSettingsCard(
                settings = settings.value,
                model = model,
            )
        }
    }
}

@Composable
internal expect fun SystemDefaultIcon()
