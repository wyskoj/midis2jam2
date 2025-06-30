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

package org.wysko.midis2jam2.ui.settings.instruments

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.keep
import midis2jam2.app.generated.resources.settings_instruments
import midis2jam2.app.generated.resources.settings_instruments_always_show_instruments
import midis2jam2.app.generated.resources.settings_instruments_always_show_instruments_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

object InstrumentsSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_instruments)) }
        ) {
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_instruments_always_show_instruments)) },
                icon = { Icon(painterResource(Res.drawable.keep), "") },
                label = { Text(stringResource(Res.string.settings_instruments_always_show_instruments_description)) },
                isEnabled = settings.value.instrumentSettings.isAlwaysShowInstruments,
                setIsEnabled = settingsModel::setAlwaysShowInstruments,
            )
        }
    }
}
