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

package org.wysko.midis2jam2.gui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import midis2jam2.generated.resources.Res
import midis2jam2.generated.resources.language
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.ApplicationScreen
import org.wysko.midis2jam2.gui.TabFactory
import org.wysko.midis2jam2.gui.components.SettingsListItem
import org.wysko.midis2jam2.gui.components.SettingsSectionHeader
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    openScreen: (ApplicationScreen) -> Unit,
) {
    val fullscreen = viewModel.isFullscreen.collectAsState()
    val isCursorLocked = viewModel.isCursorLocked.collectAsState()
    val isSpeedModifierMode = viewModel.isSpeedModifierMode.collectAsState()
    val startAutocamWithSong = viewModel.isStartAutocamWithSong.collectAsState()
    val showHud = viewModel.isShowHud.collectAsState()
    val showLyrics = viewModel.isShowLyrics.collectAsState()
    val instrumentsAlwaysVisible = viewModel.isInstrumentsAlwaysVisible.collectAsState()
    val isCameraSmooth = viewModel.isCameraSmooth.collectAsState()
    val isClassicCamera = viewModel.isClassicCamera.collectAsState()

    Column(
        modifier = Modifier.padding(top = 16.dp).verticalScroll(
            state = rememberScrollState(),
        )
    ) {
        Text(
            I18n["settings_title"].value,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ElevatedButton({
            I18n.incrementLocale()
        }, modifier = Modifier.padding(horizontal = 16.dp)) {
            val locale = I18n.currentLocale
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(painterResource(Res.drawable.language), contentDescription = null)
                Text(locale.isO3Language.uppercase(locale))
            }
        }
        SettingsSectionHeader(I18n["settings_display"].value)
        SettingsListItem(
            { Text(I18n["settings_display_fullscreen"].value) },
            { Text(I18n["settings_display_fullscreen_description"].value) },
            fullscreen,
            setState = { viewModel.setFullscreen(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_display_lockcursor"].value) },
            { Text(I18n["settings_display_lockcursor_description"].value) },
            isCursorLocked,
            setState = { viewModel.setIsCursorLocked(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_display_background"].value) },
            { Text(I18n["settings_display_background_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.backgroundConfiguration)
            }
        )
        SettingsListItem(
            { Text(I18n["settings_display_graphics"].value) },
            { Text(I18n["settings_display_graphics_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.graphicsConfiguration)
            }
        )
        SettingsSectionHeader(I18n["settings_controls"].value)
        SettingsListItem(
            { Text(I18n["settings_controls_speed_modifier_mode"].value) },
            { Text(I18n["settings_controls_speed_modifier_mode_description"].value) },
            isSpeedModifierMode,
            setState = { viewModel.setIsSpeedModifierMode(it) }
        )
        SettingsSectionHeader(I18n["settings_features"].value)
        SettingsListItem(
            { Text(I18n["settings_features_midi_device"].value) },
            { Text(I18n["settings_features_midi_device_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.midiDeviceConfiguration)
            }
        )
        SettingsListItem(
            { Text(I18n["settings_features_soundbanks"].value) },
            { Text(I18n["settings_features_soundbanks_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.soundbankConfiguration)
            }
        )
        SettingsListItem(
            { Text(I18n["settings_features_synthesizer"].value) },
            { Text(I18n["settings_features_synthesizer_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.synthesizerConfiguration)
            }
        )
        SettingsListItem(
            { Text(I18n["settings_features_start_autocam_with_song"].value) },
            { Text(I18n["settings_features_start_autocam_with_song_description"].value) },
            startAutocamWithSong,
            setState = { viewModel.setStartAutocamWithSong(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_features_hud"].value) },
            { Text(I18n["settings_features_hud_description"].value) },
            showHud,
            setState = { viewModel.setShowHud(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_features_lyrics"].value) },
            { Text(I18n["settings_features_lyrics_description"].value) },
            showLyrics,
            setState = { viewModel.setShowLyrics(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_features_lyrics_configuration"].value) },
            { Text(I18n["settings_features_lyrics_configuration_description"].value) },
            onOpenOptions = {
                openScreen(TabFactory.lyricsConfiguration)
            }
        )
        SettingsSectionHeader(I18n["settings_tweaks"].value)
        SettingsListItem(
            { Text(I18n["settings_tweaks_instruments_always_visible"].value) },
            { Text(I18n["settings_tweaks_instruments_always_visible_description"].value) },
            instrumentsAlwaysVisible,
            setState = { viewModel.setInstrumentsAlwaysVisible(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_tweaks_camera_smooth"].value) },
            { Text(I18n["settings_tweaks_camera_smooth_description"].value) },
            isCameraSmooth,
            setState = { viewModel.setIsCameraSmooth(it) }
        )
        SettingsListItem(
            { Text(I18n["settings_tweaks_classic_camera"].value) },
            { Text(I18n["settings_tweaks_classic_camera_description"].value) },
            isClassicCamera,
            setState = { viewModel.setIsClassicCamera(it) }
        )
    }
}
