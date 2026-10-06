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

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.qos.logback.core.util.EnvUtil.isWindows
import com.install4j.api.launcher.ApplicationLauncher
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.WindowMode
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.ui.common.appLocale
import org.wysko.midis2jam2.ui.common.component.SelectOption
import org.wysko.midis2jam2.util.FilesDragAndDrop
import org.wysko.midis2jam2.util.digitsOnly
import org.wysko.midis2jam2.util.isMacOs
import org.wysko.midis2jam2.util.tintEnabled
import java.io.File
import java.io.IOException
import java.util.*

private const val MinResolutionWidth = 640
private const val MinResolutionHeight = 360

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
        section(Res.string.settings_section_updates) { row { CheckForUpdates() } }
    },
    settingsPage(SettingsPage.Graphics, Res.string.settings_graphics_description) {
        section(Res.string.settings_section_window) {
            row { WindowModeSelect(settings, model) }
            row { ResolutionSelect(settings, model) }
        }
        section(Res.string.settings_section_quality) {
            row { ShadowsQualitySelect(settings, model) }
            if (!isMacOs()) {
                row { AntiAliasingQualitySelect(settings, model) }
            }
        }
        section(Res.string.settings_background) {
            row { BackgroundSelect(settings, model) }
            row(isVisible = { settings.value.backgroundSettings.type == BackgroundType.CubeMap }) {
                CubeMapCard(settings, model)
            }
        }
    },
    settingsPage(SettingsPage.Camera, Res.string.settings_camera_description) {
        section(Res.string.settings_section_autocam) {
            row { StartAutocamWithSongBooleanSelect(settings, model) }
            row { AutoCamModeSelect(settings, model) }
            row(isVisible = { settings.value.cameraSettings.autoCamMode == AutoCamMode.Smart }) {
                CinematicPacingSelect(settings, model)
            }
        }
        section(Res.string.settings_section_freecam) {
            row { IsSmoothFreecamSelect(settings, model) }
            row { FieldOfViewSelect(settings, model) }
        }
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
    settingsPage(SettingsPage.Controls, Res.string.settings_controls_description) {
        section(Res.string.settings_section_mouse_keyboard) {
            row { LockCursorBooleanSelect(settings, model) }
            row { IsSpeedModifierKeysStickyBooleanSelect(settings, model) }
        }
        section(Res.string.settings_section_gamepad) {
            row { GamepadEnabledBooleanSelect(settings, model) }
        }
    },
    settingsPage(SettingsPage.Playback, Res.string.settings_playback_description) {
        section(Res.string.settings_playback_synthesizer) {
            row { SoundbanksSelect(settings, model) }
            row { SynthesizerReverbSelect(settings, model) }
            row { SynthesizerChorusSelect(settings, model) }
        }
        section(Res.string.midi_device) {
            row { SpecificationResetSelect(settings, model) }
        }
    },
)

internal actual val deviceThemeIcon: DrawableResource
    get() = Res.drawable.computer

@Composable
internal actual fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_general_locale),
        selected = selectedLocale,
        onSelected = {
            onSelectLocale(it)
            appLocale = it
        },
        icon = Res.drawable.language,
        options = availableLocales
            .map { Locale.of(it).let { locale -> SelectOption(value = it, title = locale.getDisplayLanguage(locale)) } }
            .sortedBy { it.title.lowercase() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionSelect(settings: State<AppSettings>, model: SettingsModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isShowSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var formWidth by remember { mutableStateOf("") }
    var formHeight by remember { mutableStateOf("") }

    val resolution = settings.value.graphicsSettings.resolutionSettings
    val isWindowed = settings.value.graphicsSettings.windowMode == WindowMode.Windowed
    SettingsNavRow(
        title = stringResource(Res.string.settings_graphics_resolution),
        icon = Res.drawable.fit_screen,
        description = if (isWindowed) null else stringResource(Res.string.settings_graphics_resolution_fullscreen),
        value = when {
            !isWindowed -> null
            resolution.isUseDefaultResolution -> stringResource(Res.string.settings_graphics_resolution_default_hint)
            else -> "${resolution.resolutionWidth} × ${resolution.resolutionHeight}"
        },
        enabled = isWindowed,
        onClick = {
            isShowSheet = true
            formWidth = resolution.resolutionWidth.toString()
            formHeight = resolution.resolutionHeight.toString()
        },
    )

    if (isShowSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    isShowSheet = false

                    val width = formWidth.toIntOrNull()
                    val height = formHeight.toIntOrNull()
                    if (width != null && height != null) {
                        model.setResolution(
                            width.coerceAtLeast(MinResolutionWidth),
                            height.coerceAtLeast(MinResolutionHeight),
                        )
                    }
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_graphics_resolution_default),
                    icon = Res.drawable.fit_screen,
                    description = stringResource(Res.string.settings_graphics_resolution_default_description),
                    checked = resolution.isUseDefaultResolution,
                    onCheckedChange = model::setIsUseDefaultResolution,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isCustomResolutionEnabled = !resolution.isUseDefaultResolution
                    OutlinedTextField(
                        value = formWidth,
                        onValueChange = { formWidth = it.digitsOnly().take(4) },
                        label = { Text(stringResource(Res.string.settings_graphics_resolution_width)) },
                        isError = formWidth.isBlank(),
                        enabled = isCustomResolutionEnabled,
                    )
                    Text(
                        "×",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.tintEnabled(isCustomResolutionEnabled)
                        )
                    )
                    OutlinedTextField(
                        value = formHeight,
                        onValueChange = { formHeight = it.digitsOnly().take(4) },
                        label = { Text(stringResource(Res.string.settings_graphics_resolution_height)) },
                        isError = formHeight.isBlank(),
                        enabled = isCustomResolutionEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckForUpdates() {
    SettingsNavRow(
        title = stringResource(Res.string.about_check_for_updates),
        icon = Res.drawable.update,
        description = stringResource(Res.string.about_check_for_updates_description),
        showChevron = false,
        onClick = {
            try {
                ApplicationLauncher.launchApplication("351", null, false, null)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        },
    )
}

@Composable
private fun SpecificationResetSelect(settings: State<AppSettings>, model: SettingsModel) {
    val reset = settings.value.playbackSettings.midiSpecificationResetSettings
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_playback_midi_specification_reset),
        description = stringResource(Res.string.settings_playback_midi_specification_reset_description),
        selected = if (reset.isSendSpecificationResetMessage) reset.midiSpecification else null,
        onSelected = {
            if (it == null) {
                model.setIsSendResetMessage(false)
            } else {
                model.setIsSendResetMessage(true)
                model.setResetMessageSpecification(it)
            }
        },
        icon = Res.drawable.replace_audio,
        options = (listOf<MidiSpecification?>(null) + MidiSpecification.entries).map {
            SelectOption(value = it, title = it?.displayName ?: stringResource(Res.string.quality_none))
        },
    )
}

@Composable
private fun WindowModeSelect(settings: State<AppSettings>, model: SettingsModel) {
    val options = buildList {
        add(
            SelectOption(
                WindowMode.Windowed,
                stringResource(Res.string.settings_graphics_window_mode_windowed),
                icon = Res.drawable.monitor,
            )
        )
        // Borderless fullscreen is Windows-only; only offer real fullscreen elsewhere.
        if (isWindows()) {
            add(
                SelectOption(
                    WindowMode.BorderlessFullscreen,
                    stringResource(Res.string.settings_graphics_window_mode_borderless),
                    icon = Res.drawable.screenshot_monitor,
                )
            )
        }
        add(
            SelectOption(
                WindowMode.Fullscreen,
                stringResource(Res.string.settings_graphics_window_mode_fullscreen),
                icon = Res.drawable.fullscreen,
            )
        )
    }
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_graphics_window_mode),
        description = stringResource(Res.string.settings_graphics_window_mode_description),
        selected = settings.value.graphicsSettings.windowMode
            .let { if (it == WindowMode.BorderlessFullscreen && !isWindows()) WindowMode.Fullscreen else it },
        onSelected = model::setWindowMode,
        icon = Res.drawable.fullscreen,
        options = options,
    )
}

@Composable
private fun ShadowsQualitySelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_graphics_shadows),
        description = stringResource(Res.string.settings_graphics_shadows_description),
        selected = settings.value.graphicsSettings.shadowsSettings.shadowsQuality,
        onSelected = model::setShadowsQuality,
        icon = Res.drawable.tonality,
        options = listOf(
            SelectOption(ShadowsQuality.Fake, stringResource(Res.string.settings_graphics_shadows_none), icon = Res.drawable.close),
            SelectOption(ShadowsQuality.Low, stringResource(Res.string.quality_low), icon = Res.drawable.radio_button_unchecked),
            SelectOption(ShadowsQuality.Medium, stringResource(Res.string.quality_medium), icon = Res.drawable.star),
            SelectOption(ShadowsQuality.High, stringResource(Res.string.quality_high), icon = Res.drawable.hotel_class),
        ),
    )
}

@Composable
private fun AntiAliasingQualitySelect(settings: State<AppSettings>, model: SettingsModel) {
    val antiAliasing = settings.value.graphicsSettings.antiAliasingSettings
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_graphics_anti_aliasing),
        description = stringResource(Res.string.settings_graphics_anti_aliasing_description),
        selected = if (antiAliasing.isUseAntiAliasing) antiAliasing.antiAliasingQuality else null,
        onSelected = {
            model.setUseAntiAliasing(it != null)
            if (it != null) {
                model.setAntiAliasingQuality(it)
            }
        },
        icon = Res.drawable.high_density,
        options = listOf<SelectOption<AntiAliasingQuality?>>(
            SelectOption(null, stringResource(Res.string.quality_none), icon = Res.drawable.close),
            SelectOption(AntiAliasingQuality.Low, stringResource(Res.string.quality_low), icon = Res.drawable.radio_button_unchecked),
            SelectOption(AntiAliasingQuality.Medium, stringResource(Res.string.quality_medium), icon = Res.drawable.star),
            SelectOption(AntiAliasingQuality.High, stringResource(Res.string.quality_high), icon = Res.drawable.hotel_class),
        ),
    )
}

@Composable
private fun LockCursorBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_controls_lock_cursor),
        description = stringResource(Res.string.settings_controls_lock_cursor_description),
        icon = Res.drawable.mouse_lock,
        checked = settings.value.controlsSettings.isLockCursor,
        onCheckedChange = model::setLockCursorEnabled,
    )
}

@Composable
private fun GamepadEnabledBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_controls_gamepad_enabled),
        description = stringResource(Res.string.settings_controls_gamepad_enabled_description),
        icon = Res.drawable.gamepad,
        checked = settings.value.controlsSettings.isGamepadEnabled,
        onCheckedChange = model::setGamepadEnabled,
    )
}

@Composable
private fun IsSpeedModifierKeysStickyBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    val isSticky = settings.value.controlsSettings.isSpeedModifierKeysSticky
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_controls_sticky_speed_modifier_keys),
        description = stringResource(
            if (isSticky) Res.string.settings_controls_sticky_speed_modifier_keys_true
            else Res.string.settings_controls_sticky_speed_modifier_keys_false
        ),
        icon = Res.drawable.keyboard_lock,
        checked = isSticky,
        onCheckedChange = model::setSpeedModifierKeysSticky,
    )
}

@Composable
private fun IsSmoothFreecamSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_camera_smooth_freecam),
        description = stringResource(Res.string.settings_camera_smooth_freecam_description),
        icon = Res.drawable.video_stable,
        checked = settings.value.cameraSettings.isSmoothFreecam,
        onCheckedChange = model::setSmoothFreecam,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundbanksSelect(settings: State<AppSettings>, model: SettingsModel) {
    val soundbankExtensions = listOf("sf2", "dls")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isShowSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(soundbankExtensions),
        mode = FileKitMode.Multiple(),
    ) {
        it?.let { platformFiles ->
            model.addSoundbanks(platformFiles.map { it.file.path })
        }
    }

    SettingsNavRow(
        title = stringResource(Res.string.settings_playback_soundbanks),
        icon = Res.drawable.audio_file,
        description = stringResource(Res.string.settings_playback_soundbanks_description),
        value = settings.value.playbackSettings.soundbanksSettings.soundbanks.size.takeIf { it > 0 }?.toString(),
        onClick = { isShowSheet = true },
    )

    val dragAndDropTarget = remember {
        FilesDragAndDrop { files ->
            model.addSoundbanks(
                files.filter { soundbankExtensions.contains(it.extension.lowercase()) }.map { it.absolutePath }
            )
        }
    }

    if (isShowSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    isShowSheet = false
                }
            },
            sheetState = sheetState,
            modifier = Modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget,
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button({
                    picker.launch()
                }) {
                    Text(stringResource(Res.string.settings_playback_soundbanks_add))
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (settings.value.playbackSettings.soundbanksSettings.soundbanks.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            ) {
                                Text(
                                    stringResource(Res.string.settings_playback_soundbanks_none_loaded),
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    items(settings.value.playbackSettings.soundbanksSettings.soundbanks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = File(it).name,
                                modifier = Modifier.weight(1f, true),
                            )
                            IconButton({
                                model.removeSoundbank(it)
                            }) {
                                Icon(painterResource(Res.drawable.close), null)
                            }
                        }
                    }
                }
            }
        }
    }
}
