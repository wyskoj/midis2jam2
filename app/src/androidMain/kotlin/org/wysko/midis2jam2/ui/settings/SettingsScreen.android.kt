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

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.domain.LocaleHelper
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.ui.common.component.SelectOption
import java.util.Locale

internal actual fun settingsPages(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
): List<SettingsPageContent> = listOf(
    settingsPage(SettingsPage.General, Res.string.settings_general_description) {
        section(Res.string.settings_section_appearance) {
            row { ThemeSelect(settings, model) }
            row {
                LocaleSelect(
                    settings.value.generalSettings.locale,
                    model::setLocale,
                    screenModel.getAvailableLocales(),
                )
            }
        }
    },
    settingsPage(SettingsPage.Graphics, Res.string.settings_graphics_description_a) {
        section(Res.string.settings_section_quality) { row { ShadowsBooleanSelect(settings, model) } }
        section(Res.string.settings_background) { row { BackgroundSelect(settings, model) } }
    },
    settingsPage(SettingsPage.Camera, Res.string.settings_camera_description) {
        section(Res.string.settings_section_autocam) {
            row { StartAutocamWithSongBooleanSelect(settings, model) }
            row { AutoCamModeSelect(settings, model) }
            row(isVisible = { settings.value.cameraSettings.autoCamMode == AutoCamMode.Smart }) {
                CinematicPacingSelect(settings, model)
            }
        }
        section(Res.string.settings_section_freecam) { row { FieldOfViewSelect(settings, model) } }
    },
    settingsPage(SettingsPage.Instruments, Res.string.settings_instruments_description) {
        section(Res.string.settings_section_visibility) { row { AlwaysShowInstrumentsBooleanSelect(settings, model) } }
        section {
            row { SmartMalletsBooleanSelect(settings, model) }
            row { SmartDrumSticksBooleanSelect(settings, model) }
        }
    },
    settingsPage(SettingsPage.OnScreen, Res.string.settings_on_screen_elements_description) {
        section(Res.string.settings_section_overlays) {
            row { HudBooleanSelect(settings, model) }
            row { LyricsSwitch(settings, model) }
            row(isVisible = { settings.value.onScreenElementsSettings.lyricsSettings.isShowLyrics }) {
                LyricsSizeSelect(settings, model)
            }
        }
    },
    settingsPage(
        SettingsPage.Controls,
        Res.string.settings_controls_description_a,
        icon = Res.drawable.touch_app,
    ) {
        section(Res.string.settings_section_touch) { row { DisableTouchInputBooleanSelect(settings, model) } }
    },
    settingsPage(SettingsPage.Playback, Res.string.settings_playback_description) {
        section(Res.string.settings_playback_synthesizer) {
            row { SoundbanksSelect() }
            row { SynthesizerReverbSelect(settings, model) }
            row { SynthesizerChorusSelect(settings, model) }
        }
    },
)

internal actual val deviceThemeIcon: DrawableResource
    get() = Res.drawable.android

@Composable
internal actual fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
) {
    val systemInteraction = koinInject<SystemInteractionService>()
    val context = LocalContext.current

    when (CompatLibrary.useLegacyLanguageSelect) {
        true -> {
            val options = availableLocales.map {
                SelectOption(value = it, title = Locale(it).displayName)
            }
            // Forces recomposition on locale change (12-)
            AppCompatDelegate.getApplicationLocales().get(0)
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_general_locale),
                selected = selectedLocale,
                onSelected = {
                    onSelectLocale(it)
                    LocaleHelper.updateLocale(context, it)
                },
                icon = Res.drawable.language,
                options = options,
            )
        }

        false -> {
            SettingsNavRow(
                title = stringResource(Res.string.settings_general_locale),
                icon = Res.drawable.language,
                value = systemInteraction.getLocale().displayLanguage,
                onClick = { systemInteraction.openSystemLanguageSettings() },
            )
        }
    }
}

@Composable
private fun DisableTouchInputBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_controls_disable_touch),
        description = stringResource(Res.string.settings_controls_disable_touch_description),
        icon = Res.drawable.hand_gesture_off,
        checked = settings.value.controlsSettings.isDisableTouchInput,
        onCheckedChange = model::setDisableTouchInput,
    )
}

@Composable
private fun SoundbanksSelect() {
    val navigator = LocalNavigator.currentOrThrow
    SettingsNavRow(
        title = stringResource(Res.string.settings_playback_soundbanks),
        icon = Res.drawable.audio_file,
        description = stringResource(Res.string.settings_playback_soundbanks_description),
        onClick = { navigator.push(SoundbanksScreen) },
    )
}
