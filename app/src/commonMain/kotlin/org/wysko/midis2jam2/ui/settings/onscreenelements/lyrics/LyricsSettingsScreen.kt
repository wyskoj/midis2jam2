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

package org.wysko.midis2jam2.ui.settings.onscreenelements.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.format_size
import midis2jam2.app.generated.resources.lyrics
import midis2jam2.app.generated.resources.ok
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics_description
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics_scale_template
import midis2jam2.app.generated.resources.settings_onscreenelements_lyrics_size
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

object LyricsSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics)) },
        ) {
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics)) },
                icon = { Icon(painterResource(Res.drawable.lyrics), "") },
                label = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_description)) },
                isEnabled = settings.value.onScreenElementsSettings.lyricsSettings.isShowLyrics,
                setIsEnabled = settingsModel::setShowLyrics
            )

            var showDialog by remember { mutableStateOf(false) }
            var formLyricsSize by remember {
                mutableStateOf(
                    settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize
                )
            }
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_size)) },
                icon = { Icon(painterResource(Res.drawable.format_size), "") },
                label = {
                    Text(
                        stringResource(
                            Res.string.settings_onscreenelements_lyrics_scale_template,
                            settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize
                        )
                    )
                },
                onClick = {
                    showDialog = true
                }
            )

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        formLyricsSize = settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize
                    },
                    title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_size)) },
                    text = {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Slider(
                                value = formLyricsSize.toFloat(),
                                onValueChange = { formLyricsSize = it.toDouble() },
                                valueRange = 0.5f..2.5f,
                                steps = 3,
                            )
                            Text(
                                stringResource(
                                    Res.string.settings_onscreenelements_lyrics_scale_template,
                                    formLyricsSize
                                ),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            settingsModel.setLyricsSize(formLyricsSize)
                            showDialog = false
                        }) {
                            Text(stringResource(Res.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                            formLyricsSize = settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize
                        }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                )
            }
        }
    }
}
