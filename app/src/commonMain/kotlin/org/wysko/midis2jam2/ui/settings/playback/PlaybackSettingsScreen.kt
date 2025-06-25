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

package org.wysko.midis2jam2.ui.settings.playback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import midis2jam2.app.generated.resources.audio_file
import midis2jam2.app.generated.resources.replace_audio
import midis2jam2.app.generated.resources.settings_playback
import midis2jam2.app.generated.resources.settings_playback_midi_specification_reset
import midis2jam2.app.generated.resources.settings_playback_soundbanks
import midis2jam2.app.generated.resources.settings_playback_soundbanks_description
import midis2jam2.app.generated.resources.settings_playback_synthesizer
import midis2jam2.app.generated.resources.settings_playback_synthesizer_description
import midis2jam2.app.generated.resources.tune
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.ui.common.component.SpecificationIcon
import org.wysko.midis2jam2.ui.common.component.settings.SettingsGenericCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOption
import org.wysko.midis2jam2.ui.common.component.settings.SettingsOptionsCard
import org.wysko.midis2jam2.ui.common.component.settings.SettingsScaffold
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.playback.soundbanks.SoundbanksSettingsScreen
import org.wysko.midis2jam2.ui.settings.playback.synthesizer.SynthesizerSettingsScreen

object PlaybackSettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsModel>()
        val model = koinScreenModel<PlaybackSettingsScreenModel>()
        val settings = settingsModel.appSettings.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        val specificationOptions = listOf<SettingsOption<MidiSpecification?>>(
            SettingsOption(
                value = null,
                title = "None",
                icon = { Icon(Icons.Default.Close, "") },
            ),
            SettingsOption(
                value = MidiSpecification.GeneralMidi,
                title = "General MIDI",
                icon = { SpecificationIcon(MidiSpecification.GeneralMidi) },
            ),
            SettingsOption(
                value = MidiSpecification.ExtendedGeneral,
                title = "Extended General MIDI",
                icon = { SpecificationIcon(MidiSpecification.ExtendedGeneral) },
            ),
            SettingsOption(
                value = MidiSpecification.GeneralStandard,
                title = "General Standard MIDI",
                icon = { SpecificationIcon(MidiSpecification.GeneralStandard) },
            ),
        )

        SettingsScaffold(
            title = { Text(stringResource(Res.string.settings_playback)) }
        ) {
            SettingsOptionsCard(
                title = { Text(stringResource(Res.string.settings_playback_midi_specification_reset)) },
                icon = { Icon(painterResource(Res.drawable.replace_audio), "") },
                options = specificationOptions,
                selectedOption = settings.value.playbackSettings.midiSpecificationResetSettings.midiSpecification,
                onOptionSelected = {
                    if (it == null) {
                        settingsModel.setIsSendResetMessage(false)
                    } else {
                        settingsModel.setIsSendResetMessage(true)
                        settingsModel.setResetMessageSpecification(it)
                    }
                },
                label = {
                    with(settings.value.playbackSettings.midiSpecificationResetSettings) {
                        Text(
                            model.midiSpecificationResetLabel(
                                isEnabled = isSendSpecificationResetMessage,
                                specification = midiSpecification
                            )
                        )
                    }
                }
            )
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_playback_synthesizer)) },
                icon = { Icon(painterResource(Res.drawable.tune), "") },
                label = { Text(stringResource(Res.string.settings_playback_synthesizer_description)) },
                onClick = {
                    navigator.push(SynthesizerSettingsScreen)
                }
            )
            SettingsGenericCard(
                title = { Text(stringResource(Res.string.settings_playback_soundbanks)) },
                icon = { Icon(painterResource(Res.drawable.audio_file), "") },
                label = { Text(stringResource(Res.string.settings_playback_soundbanks_description)) },
                onClick = {
                    navigator.push(SoundbanksSettingsScreen)
                }
            )
        }
    }
}