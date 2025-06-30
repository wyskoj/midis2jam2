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

package org.wysko.midis2jam2.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.display_settings
import midis2jam2.app.generated.resources.keyboard
import midis2jam2.app.generated.resources.media_output
import midis2jam2.app.generated.resources.piano
import midis2jam2.app.generated.resources.screenshot_monitor
import midis2jam2.app.generated.resources.settings_background
import midis2jam2.app.generated.resources.settings_background_description
import midis2jam2.app.generated.resources.settings_camera
import midis2jam2.app.generated.resources.settings_camera_description
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_description
import midis2jam2.app.generated.resources.settings_fill
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_description
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_graphics_description
import midis2jam2.app.generated.resources.settings_instruments
import midis2jam2.app.generated.resources.settings_instruments_description
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_on_screen_elements_description
import midis2jam2.app.generated.resources.settings_playback
import midis2jam2.app.generated.resources.settings_playback_description
import midis2jam2.app.generated.resources.tab_settings
import midis2jam2.app.generated.resources.videocam
import midis2jam2.app.generated.resources.wallpaper
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.ui.settings.background.BackgroundSettingsScreen
import org.wysko.midis2jam2.ui.settings.camera.CameraSettingsScreen
import org.wysko.midis2jam2.ui.settings.controls.ControlsSettingsScreen
import org.wysko.midis2jam2.ui.settings.general.GeneralSettingsScreen
import org.wysko.midis2jam2.ui.settings.graphics.GraphicsSettingsScreen
import org.wysko.midis2jam2.ui.settings.instruments.InstrumentsSettingsScreen
import org.wysko.midis2jam2.ui.settings.onscreenelements.OnScreenElementsSettingsScreen
import org.wysko.midis2jam2.ui.settings.playback.PlaybackSettingsScreen

internal actual val graphicsCategoryDescription: StringResource
    get() = Res.string.settings_graphics_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun SettingsScreenScaffold(content: @Composable (() -> Unit)) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            content()
        }
    }
}

actual val categoryGroups: List<List<SettingsCategoryCardProps>>
    @Composable
    get() = listOf(
        listOf(
            SettingsCategoryCardProps(
                title = Res.string.settings_general,
                description = Res.string.settings_general_description,
                icon = Res.drawable.settings_fill,
                GeneralSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_graphics,
                description = graphicsCategoryDescription,
                icon = Res.drawable.display_settings,
                GraphicsSettingsScreen
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_background,
                description = Res.string.settings_background_description,
                icon = Res.drawable.wallpaper,
                BackgroundSettingsScreen
            )
        ),
        listOf(
            SettingsCategoryCardProps(
                title = Res.string.settings_controls,
                description = Res.string.settings_controls_description,
                icon = Res.drawable.keyboard,
                ControlsSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_playback,
                description = Res.string.settings_playback_description,
                icon = Res.drawable.media_output,
                PlaybackSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_on_screen_elements,
                description = Res.string.settings_on_screen_elements_description,
                icon = Res.drawable.screenshot_monitor,
                OnScreenElementsSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_camera,
                description = Res.string.settings_camera_description,
                icon = Res.drawable.videocam,
                CameraSettingsScreen,
            ),
            SettingsCategoryCardProps(
                title = Res.string.settings_instruments,
                description = Res.string.settings_instruments_description,
                icon = Res.drawable.piano,
                InstrumentsSettingsScreen,
            ),
        ),
    )