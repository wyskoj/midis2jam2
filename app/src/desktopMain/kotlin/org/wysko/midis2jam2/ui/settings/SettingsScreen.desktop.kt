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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.install4j.api.launcher.ApplicationLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.about_check_for_updates
import midis2jam2.app.generated.resources.about_check_for_updates_description
import midis2jam2.app.generated.resources.audio_file
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.computer
import midis2jam2.app.generated.resources.fit_screen
import midis2jam2.app.generated.resources.fullscreen
import midis2jam2.app.generated.resources.high_density
import midis2jam2.app.generated.resources.hotel_class
import midis2jam2.app.generated.resources.keyboard_lock
import midis2jam2.app.generated.resources.language
import midis2jam2.app.generated.resources.midi_device
import midis2jam2.app.generated.resources.mouse_lock
import midis2jam2.app.generated.resources.quality_high
import midis2jam2.app.generated.resources.quality_low
import midis2jam2.app.generated.resources.quality_medium
import midis2jam2.app.generated.resources.quality_none
import midis2jam2.app.generated.resources.radio_button_unchecked
import midis2jam2.app.generated.resources.replace_audio
import midis2jam2.app.generated.resources.settings_camera
import midis2jam2.app.generated.resources.settings_camera_smooth_freecam
import midis2jam2.app.generated.resources.settings_camera_smooth_freecam_description
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_lock_cursor
import midis2jam2.app.generated.resources.settings_controls_lock_cursor_description
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys_false
import midis2jam2.app.generated.resources.settings_controls_sticky_speed_modifier_keys_true
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_locale
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_graphics_anti_aliasing
import midis2jam2.app.generated.resources.settings_graphics_anti_aliasing_description
import midis2jam2.app.generated.resources.settings_graphics_fullscreen
import midis2jam2.app.generated.resources.settings_graphics_fullscreen_description
import midis2jam2.app.generated.resources.settings_graphics_resolution
import midis2jam2.app.generated.resources.settings_graphics_resolution_default
import midis2jam2.app.generated.resources.settings_graphics_resolution_default_description
import midis2jam2.app.generated.resources.settings_graphics_resolution_default_hint
import midis2jam2.app.generated.resources.settings_graphics_resolution_fullscreen
import midis2jam2.app.generated.resources.settings_graphics_resolution_height
import midis2jam2.app.generated.resources.settings_graphics_resolution_width
import midis2jam2.app.generated.resources.settings_graphics_shadows
import midis2jam2.app.generated.resources.settings_graphics_shadows_description
import midis2jam2.app.generated.resources.settings_graphics_shadows_none
import midis2jam2.app.generated.resources.settings_instruments
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_playback_midi_specification_reset
import midis2jam2.app.generated.resources.settings_playback_midi_specification_reset_description
import midis2jam2.app.generated.resources.settings_playback_soundbanks
import midis2jam2.app.generated.resources.settings_playback_soundbanks_add
import midis2jam2.app.generated.resources.settings_playback_soundbanks_description
import midis2jam2.app.generated.resources.settings_playback_soundbanks_none_loaded
import midis2jam2.app.generated.resources.settings_playback_synthesizer
import midis2jam2.app.generated.resources.star
import midis2jam2.app.generated.resources.tonality
import midis2jam2.app.generated.resources.update
import midis2jam2.app.generated.resources.video_stable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.AntiAliasingSettings.AntiAliasingQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.ShadowsSettings.ShadowsQuality
import org.wysko.midis2jam2.domain.settings.AppSettings.PlaybackSettings.MidiSpecificationResetSettings.MidiSpecification
import org.wysko.midis2jam2.ui.common.appLocale
import org.wysko.midis2jam2.ui.common.component.CategoryHeader
import org.wysko.midis2jam2.ui.common.component.SelectOption
import org.wysko.midis2jam2.ui.common.component.SelectRow
import org.wysko.midis2jam2.ui.common.component.SwitchRow
import org.wysko.midis2jam2.ui.common.component.UnitRow
import org.wysko.midis2jam2.util.FilesDragAndDrop
import org.wysko.midis2jam2.util.digitsOnly
import org.wysko.midis2jam2.util.tintEnabled
import java.io.File
import java.io.IOException
import java.util.*

internal actual val deviceThemeIcon: DrawableResource
    get() = Res.drawable.computer

@Composable
internal actual fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
) {
    val options = availableLocales.map {
        SelectOption(
            value = it,
            title = Locale.of(it).displayLanguage,
        )
    }
    SelectRow(
        selectedLocale,
        {
            onSelectLocale(it)
            appLocale = it
        },
        options = options,
        title = { Text(stringResource(Res.string.settings_general_locale)) },
        icon = Res.drawable.language,
    )
}

internal actual fun LazyListScope.SettingsScreenContent(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_general))
    }
    item {
        ThemeSelect(settings, model)
    }
    item {
        LocaleSelect(
            settings.value.generalSettings.locale,
            model::setLocale,
            screenModel.getAvailableLocales()
        )
    }
    item {
        CheckForUpdates()
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_graphics))
    }
    windowSettings(settings, model)
    item {
        ShadowsQualitySelect(settings, model)
    }
    item {
        AntiAliasingQualitySelect(settings, model)
    }
    item {
        BackgroundSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_on_screen_elements))
    }
    LyricsSelect(settings, model)
    item {
        HudBooleanSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_instruments))
    }
    item {
        AlwaysShowInstrumentsBooleanSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_controls))
    }
    item {
        LockCursorBooleanSelect(settings, model)
    }
    item {
        IsSpeedModifierKeysStickyBooleanSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_playback_synthesizer))
    }
    item {
        SoundbanksSelect(settings, model)
    }
    item {
        SynthesizerReverbSelect(settings, model)
    }
    item {
        SynthesizerChorusSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.midi_device))
    }
    item {
        SpecificationResetSelect(settings, model)
    }
    item { // stickyHeader
        CategoryHeader(stringResource(Res.string.settings_camera))
    }
    item {
        StartAutocamWithSongBooleanSelect(settings, model)
    }
    item {
        IsClassicAutoCamBooleanSelect(settings, model)
    }
    item {
        IsSmoothFreecamSelect(settings, model)
    }
    item {
        FieldOfViewSelect(settings, model)
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.windowSettings(settings: State<AppSettings>, model: SettingsModel) {
    item {
        SwitchRow(
            checked = settings.value.graphicsSettings.isFullscreen,
            onCheckedChange = model::setIsFullscreen,
            title = { Text(stringResource(Res.string.settings_graphics_fullscreen)) },
            label = { Text(stringResource(Res.string.settings_graphics_fullscreen_description)) },
            icon = Res.drawable.fullscreen,
        )
    }
    item {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var isShowSheet by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        var formWidth by remember { mutableStateOf("") }
        var formHeight by remember { mutableStateOf("") }

        UnitRow(
            title = { Text(stringResource(Res.string.settings_graphics_resolution)) },
            label = {
                Text(
                    text = when {
                        settings.value.graphicsSettings.isFullscreen -> stringResource(
                            Res.string.settings_graphics_resolution_fullscreen
                        )

                        else -> when (settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution) {
                            true -> stringResource(Res.string.settings_graphics_resolution_default_hint)
                            false -> "${settings.value.graphicsSettings.resolutionSettings.resolutionWidth} × ${settings.value.graphicsSettings.resolutionSettings.resolutionHeight}"
                        }
                    }
                )
            },
            icon = Res.drawable.fit_screen,
            enabled = !settings.value.graphicsSettings.isFullscreen,
        ) {
            isShowSheet = true
            with(settings.value.graphicsSettings.resolutionSettings) {
                formWidth = resolutionWidth.toString()
                formHeight = resolutionHeight.toString()
            }
        }

        if (isShowSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch {
                        sheetState.hide()
                        isShowSheet = false

                        if (formWidth.isNotBlank() && formHeight.isNotBlank()) {
                            model.setResolution(formWidth.toInt(), formHeight.toInt())
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
                    SwitchRow(
                        checked = settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution,
                        onCheckedChange = model::setIsUseDefaultResolution,
                        title = { Text(stringResource(Res.string.settings_graphics_resolution_default)) },
                        label = { Text(stringResource(Res.string.settings_graphics_resolution_default_description)) },
                        icon = Res.drawable.fit_screen,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isCustomResolutionEnabled =
                            !settings.value.graphicsSettings.resolutionSettings.isUseDefaultResolution
                        OutlinedTextField(
                            value = formWidth,
                            onValueChange = {
                                formWidth = it.digitsOnly().take(4)
                            },
                            label = {
                                Text(stringResource(Res.string.settings_graphics_resolution_width))
                            },
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
                            onValueChange = {
                                formHeight = it.digitsOnly().take(4)
                            },
                            label = {
                                Text(stringResource(Res.string.settings_graphics_resolution_height))
                            },
                            isError = formWidth.isBlank(),
                            enabled = isCustomResolutionEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckForUpdates() {
    UnitRow(
        title = { Text(stringResource(Res.string.about_check_for_updates)) },
        label = { Text(stringResource(Res.string.about_check_for_updates_description)) },
        icon = Res.drawable.update,
    ) {
        try {
            ApplicationLauncher.launchApplication("351", null, false, null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun SpecificationResetSelect(settings: State<AppSettings>, model: SettingsModel) {
    val options = (listOf(null) + MidiSpecification.entries.toList()).map {
        SelectOption(
            value = it,
            title = it?.displayName ?: stringResource(Res.string.quality_none),
        )
    }

    SelectRow(
        option = when (settings.value.playbackSettings.midiSpecificationResetSettings.isSendSpecificationResetMessage) {
            true -> settings.value.playbackSettings.midiSpecificationResetSettings.midiSpecification
            false -> null
        },
        onOptionSelected = {
            if (it == null) {
                model.setIsSendResetMessage(false)
            } else {
                model.setIsSendResetMessage(true)
                model.setResetMessageSpecification(it)
            }
        },
        options,
        title = { Text(stringResource(Res.string.settings_playback_midi_specification_reset)) },
        icon = Res.drawable.replace_audio,
        description = stringResource(Res.string.settings_playback_midi_specification_reset_description),
    )
}

@Composable
private fun ShadowsQualitySelect(settings: State<AppSettings>, model: SettingsModel) {
    val options = listOf(
        SelectOption(
            value = ShadowsQuality.Fake,
            title = stringResource(Res.string.settings_graphics_shadows_none),
            icon = Res.drawable.close
        ),
        SelectOption(
            value = ShadowsQuality.Low,
            title = stringResource(Res.string.quality_low),
            icon = Res.drawable.radio_button_unchecked
        ),
        SelectOption(
            value = ShadowsQuality.Medium,
            title = stringResource(Res.string.quality_medium),
            icon = Res.drawable.star
        ),
        SelectOption(
            value = ShadowsQuality.High,
            title = stringResource(Res.string.quality_high),
            icon = Res.drawable.hotel_class
        ),
    )
    SelectRow(
        settings.value.graphicsSettings.shadowsSettings.shadowsQuality,
        model::setShadowsQuality,
        options,
        title = { Text(stringResource(Res.string.settings_graphics_shadows)) },
        icon = Res.drawable.tonality,
        description = stringResource(Res.string.settings_graphics_shadows_description),
    )
}

@Composable
private fun AntiAliasingQualitySelect(settings: State<AppSettings>, model: SettingsModel) {
    val options = listOf<SelectOption<AntiAliasingQuality?>>(
        SelectOption(
            value = null,
            title = stringResource(Res.string.quality_none),
            icon = Res.drawable.close
        ),
        SelectOption(
            value = AntiAliasingQuality.Low,
            title = stringResource(Res.string.quality_low),
            icon = Res.drawable.radio_button_unchecked
        ),
        SelectOption(
            value = AntiAliasingQuality.Medium,
            title = stringResource(Res.string.quality_medium),
            icon = Res.drawable.star
        ),
        SelectOption(
            value = AntiAliasingQuality.High,
            title = stringResource(Res.string.quality_high),
            icon = Res.drawable.hotel_class
        ),
    )
    SelectRow(
        option = if (settings.value.graphicsSettings.antiAliasingSettings.isUseAntiAliasing) {
            settings.value.graphicsSettings.antiAliasingSettings.antiAliasingQuality
        } else {
            null
        },
        {
            model.setUseAntiAliasing(it != null)
            if (it != null) {
                model.setAntiAliasingQuality(it)
            }
        },
        options,
        title = { Text(stringResource(Res.string.settings_graphics_anti_aliasing)) },
        icon = Res.drawable.high_density,
        description = stringResource(Res.string.settings_graphics_anti_aliasing_description),
    )
}

@Composable
private fun LockCursorBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SwitchRow(
        settings.value.controlsSettings.isLockCursor,
        model::setLockCursorEnabled,
        title = { Text(stringResource(Res.string.settings_controls_lock_cursor)) },
        label = { Text(stringResource(Res.string.settings_controls_lock_cursor_description)) },
        icon = Res.drawable.mouse_lock
    )
}

@Composable
private fun IsSpeedModifierKeysStickyBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SwitchRow(
        settings.value.controlsSettings.isSpeedModifierKeysSticky,
        model::setSpeedModifierKeysSticky,
        title = { Text(stringResource(Res.string.settings_controls_sticky_speed_modifier_keys)) },
        label = {
            Text(
                stringResource(
                    when (settings.value.controlsSettings.isSpeedModifierKeysSticky) {
                        true -> Res.string.settings_controls_sticky_speed_modifier_keys_true
                        false -> Res.string.settings_controls_sticky_speed_modifier_keys_false
                    }
                )
            )
        },
        icon = Res.drawable.keyboard_lock
    )
}

@Composable
private fun IsSmoothFreecamSelect(settings: State<AppSettings>, model: SettingsModel) {
    SwitchRow(
        settings.value.cameraSettings.isSmoothFreecam,
        model::setSmoothFreecam,
        title = { Text(stringResource(Res.string.settings_camera_smooth_freecam)) },
        label = {
            Text(stringResource(Res.string.settings_camera_smooth_freecam_description))
        },
        icon = Res.drawable.video_stable
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
        type = PickerType.File(soundbankExtensions),
        mode = PickerMode.Multiple(),
        title = "Select soundbanks",
    ) {
        it?.let { platformFiles ->
            model.addSoundbanks(platformFiles.map { it.file.path })
        }
    }

    UnitRow(
        title = { Text(stringResource(Res.string.settings_playback_soundbanks)) },
        label = { Text(stringResource(Res.string.settings_playback_soundbanks_description)) },
        icon = Res.drawable.audio_file
    ) {
        isShowSheet = true
    }

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
