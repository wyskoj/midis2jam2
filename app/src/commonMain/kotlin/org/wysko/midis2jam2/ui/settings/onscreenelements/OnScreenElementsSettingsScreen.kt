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

package org.wysko.midis2jam2.ui.settings.onscreenelements

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.browse_activity
import midis2jam2.app.generated.resources.lyrics
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_onscreenelements_hud
import midis2jam2.app.generated.resources.settings_onscreenelements_hud_description
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.onscreenelements.lyrics.LyricsSettingsScreen

object OnScreenElementsSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_on_screen_elements)) }
        ) {
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics)) },
                icon = { Icon(painterResource(Res.drawable.lyrics), "") },
                label = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_description)) },
                onClick = {
                    navigator.push(LyricsSettingsScreen)
                }
            )
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_onscreenelements_hud)) },
                icon = { Icon(painterResource(Res.drawable.browse_activity), "") },
                label = { Text(stringResource(Res.string.settings_onscreenelements_hud_description)) },
                isEnabled = settings.value.onScreenElementsSettings.isShowHeadsUpDisplay,
                setIsEnabled = settingsModel::setShowHeadsUpDisplay,
            )
        }
    }
}
