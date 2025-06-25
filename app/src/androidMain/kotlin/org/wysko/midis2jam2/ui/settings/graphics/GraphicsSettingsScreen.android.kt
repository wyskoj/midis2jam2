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

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_graphics_shadows_description_a
import midis2jam2.app.generated.resources.settings_graphics_shadows_description_hint_a
import midis2jam2.app.generated.resources.tonality
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

@Composable
internal actual fun GraphicsSettingsScreenContent() {
    val navigator = LocalNavigator.currentOrThrow
    val settingsModel = GraphicsSettingsScreen.koinScreenModel<SettingsModel>()
    val model = GraphicsSettingsScreen.koinScreenModel<GraphicsSettingsScreenModel>()
    val settings = settingsModel.appSettings.collectAsState()

    SettingsScaffold(
        title = { Text(stringResource(Res.string.settings_graphics)) }
    ) {
        SettingsBooleanCard(
            title = { Text(stringResource(Res.string.settings_graphics_shadows_description_a)) },
            icon = { Icon(painterResource(Res.drawable.tonality), "") },
            isEnabled = settings.value.graphicsSettings.shadowsSettings.isUseShadows,
            label = { Text(stringResource(Res.string.settings_graphics_shadows_description_hint_a)) },
            setIsEnabled = { settingsModel.setUseShadows(it) },
        )
    }
}