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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.fit_screen
import midis2jam2.app.generated.resources.ok
import midis2jam2.app.generated.resources.responsive_layout
import midis2jam2.app.generated.resources.settings_graphics_resolution
import midis2jam2.app.generated.resources.settings_graphics_resolution_custom
import midis2jam2.app.generated.resources.settings_graphics_resolution_default
import midis2jam2.app.generated.resources.settings_graphics_resolution_default_description
import midis2jam2.app.generated.resources.settings_graphics_resolution_height
import midis2jam2.app.generated.resources.settings_graphics_resolution_width
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.common.component.settings.SettingsBooleanCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel

object ResolutionSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val model = koinScreenModel<ResolutionSettingsScreenModel>()
        val settingsModel = koinScreenModel<SettingsModel>()
        val settings = settingsModel.appSettings.collectAsState()

        SettingsScaffold(title = { Text(stringResource(Res.string.settings_graphics_resolution)) }) {
            SettingsBooleanCard(
                title = { Text(stringResource(Res.string.settings_graphics_resolution_default)) },
                label = { Text(stringResource(Res.string.settings_graphics_resolution_default_description)) },
                icon = { Icon(painterResource(Res.drawable.fit_screen), "") },
                isEnabled = settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution,
                setIsEnabled = settingsModel::setIsUseDefaultResolution,
            )

            var showDialog by remember { mutableStateOf(false) }
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_graphics_resolution_custom)) },
                icon = { Icon(painterResource(Res.drawable.responsive_layout), "") },
                label = {
                    Text(
                        model.formatResolution(
                            settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution,
                            settings.value.graphicsSettings.resolutionSettings.resolutionWidth,
                            settings.value.graphicsSettings.resolutionSettings.resolutionHeight
                        )
                    )
                },
                onClick = {
                    showDialog = true
                },
                enabled = !settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution,
            )

            var formResolutionWidth by remember { mutableStateOf("") }
            var formResolutionHeight by remember { mutableStateOf("") }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(stringResource(Res.string.settings_graphics_resolution)) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = formResolutionWidth,
                                onValueChange = { formResolutionWidth = it },
                                label = { Text(stringResource(Res.string.settings_graphics_resolution_width)) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = formResolutionHeight,
                                onValueChange = { formResolutionHeight = it },
                                label = { Text(stringResource(Res.string.settings_graphics_resolution_height)) },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            settingsModel.setResolution(
                                formResolutionWidth.toIntOrNull() ?: 0,
                                formResolutionHeight.toIntOrNull() ?: 0
                            )
                            showDialog = false
                        }, enabled = model.validateResolution(formResolutionWidth, formResolutionHeight)) {
                            Text(stringResource(Res.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    },
                )
            }
        }
    }
}