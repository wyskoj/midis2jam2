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

import Platform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.Color
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.CubeMap
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.Default
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.AutoCamMode
import org.wysko.midis2jam2.domain.settings.AppSettings.CameraSettings.CinematicSettings.CinematicPacing
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.ui.common.component.*
import kotlin.math.roundToInt

/*
 * The rows shared by every platform. Each reads its value from [settings] inside the composition,
 * so it recomposes when the setting changes.
 */

@Composable
internal fun ThemeSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_general_theme),
        selected = settings.value.generalSettings.theme,
        onSelected = model::setAppTheme,
        options = listOf(
            SelectOption(
                AppTheme.LIGHT,
                stringResource(Res.string.settings_general_theme_light),
                icon = Res.drawable.light_mode
            ),
            SelectOption(
                AppTheme.DARK,
                stringResource(Res.string.settings_general_theme_dark),
                icon = Res.drawable.dark_mode
            ),
            SelectOption(
                AppTheme.SYSTEM_DEFAULT,
                stringResource(Res.string.settings_general_theme_system),
                icon = deviceThemeIcon
            ),
        ),
    )
}

@Composable
internal fun ShadowsBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_graphics_shadows_description_a),
        description = stringResource(Res.string.settings_graphics_shadows_description_hint_a),
        icon = Res.drawable.tonality,
        checked = settings.value.graphicsSettings.shadowsSettings.isUseShadows,
        onCheckedChange = model::setUseShadows,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    var showColorSelectModal by remember { mutableStateOf(false) }
    val colorSelectSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val background = settings.value.backgroundSettings
    val color = background.color
    val colorDescription = stringResource(Res.string.settings_background_type_color)

    SettingsChoiceRow(
        title = stringResource(Res.string.settings_background),
        description = stringResource(Res.string.settings_background_description),
        selected = background.type,
        onSelected = {
            model.setBackgroundType(it)
            if (it == Color) showColorSelectModal = true
        },
        options = buildList {
            add(
                SelectOption(
                    Default,
                    stringResource(Res.string.settings_background_type_default),
                    icon = Res.drawable.wallpaper
                )
            )
            add(
                SelectOption(
                    Color,
                    stringResource(Res.string.settings_background_type_color),
                    icon = Res.drawable.palette
                )
            )
            if (Platform.current() == Platform.Desktop) {
                add(
                    SelectOption(
                        CubeMap,
                        stringResource(Res.string.settings_background_type_cubemap),
                        icon = Res.drawable.image
                    )
                )
            }
        },
        extraTrailing = {
            when (background.type) {
                Color -> Surface(
                    onClick = { showColorSelectModal = true },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color(color),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(Modifier.semantics { contentDescription = colorDescription })
                }

                else -> Unit
            }
        },
    )

    if (showColorSelectModal) {
        ModalBottomSheet(
            onDismissRequest = { showColorSelectModal = false },
            sheetState = colorSelectSheetState,
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ColorPicker(
                    color = color,
                    setColor = model::setBackgroundColor,
                )
            }
        }
    }
}

@Composable
internal fun HudBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_onscreenelements_hud),
        description = stringResource(Res.string.settings_onscreenelements_hud_description),
        icon = Res.drawable.browse_activity,
        checked = settings.value.onScreenElementsSettings.isShowHeadsUpDisplay,
        onCheckedChange = model::setShowHeadsUpDisplay,
    )
}

@Composable
internal fun LyricsSwitch(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_onscreenelements_lyrics),
        description = stringResource(Res.string.settings_onscreenelements_lyrics_description),
        icon = Res.drawable.lyrics,
        checked = settings.value.onScreenElementsSettings.lyricsSettings.isShowLyrics,
        onCheckedChange = model::setShowLyrics,
    )
}

/** Shown beneath [LyricsSwitch]; the entry is hidden while lyrics are off. */
@Composable
internal fun LyricsSizeSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_onscreenelements_lyrics_size),
        selected = settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize,
        onSelected = model::setLyricsSize,
        icon = Res.drawable.format_size,
        options = listOf(
            SelectOption(0.5, stringResource(Res.string.settings_onscreenelements_lyrics_size_smaller)),
            SelectOption(1.0, stringResource(Res.string.settings_onscreenelements_lyrics_size_small)),
            SelectOption(1.5, stringResource(Res.string.settings_onscreenelements_lyrics_size_default)),
            SelectOption(2.0, stringResource(Res.string.settings_onscreenelements_lyrics_size_large)),
            SelectOption(2.5, stringResource(Res.string.settings_onscreenelements_lyrics_size_larger)),
        ),
    )
}

@Composable
internal fun AlwaysShowInstrumentsBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_instruments_always_show_instruments),
        description = stringResource(Res.string.settings_instruments_always_show_instruments_description),
        icon = Res.drawable.keep,
        checked = settings.value.instrumentSettings.isAlwaysShowInstruments,
        onCheckedChange = model::setAlwaysShowInstruments,
    )
}

@Composable
internal fun SmartMalletsBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_instruments_smart_mallets),
        description = stringResource(Res.string.settings_instruments_smart_mallets_description),
        icon = Res.drawable.swap_horiz,
        checked = settings.value.instrumentSettings.isSmartMallets,
        onCheckedChange = model::setSmartMallets,
    )
}

@Composable
internal fun SmartDrumSticksBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_instruments_smart_drum_sticks),
        description = stringResource(Res.string.settings_instruments_smart_drum_sticks_description),
        icon = Res.drawable.swap_horiz,
        checked = settings.value.instrumentSettings.isSmartDrumSticks,
        onCheckedChange = model::setSmartDrumSticks,
    )
}

@Composable
internal fun StartAutocamWithSongBooleanSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_camera_start_autocam_with_song),
        description = stringResource(Res.string.settings_camera_start_autocam_with_song_description),
        icon = Res.drawable.motion_photos_auto,
        checked = settings.value.cameraSettings.isStartAutocamWithSong,
        onCheckedChange = model::setStartAutocamWithSong,
    )
}

@Composable
internal fun AutoCamModeSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_camera_autocam_mode),
        selected = settings.value.cameraSettings.autoCamMode,
        onSelected = model::setAutoCamMode,
        options = listOf(
            SelectOption(AutoCamMode.Smart, stringResource(Res.string.settings_camera_autocam_mode_smart)),
            SelectOption(AutoCamMode.Classic, stringResource(Res.string.settings_camera_autocam_mode_classic)),
            SelectOption(AutoCamMode.Legacy, stringResource(Res.string.settings_camera_autocam_mode_legacy)),
        ),
    )
}

@Composable
internal fun CinematicPacingSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsChoiceRow(
        title = stringResource(Res.string.settings_camera_cinematic_pacing),
        selected = settings.value.cameraSettings.cinematicSettings.pacing,
        onSelected = model::setCinematicPacing,
        options = listOf(
            SelectOption(CinematicPacing.Relaxed, stringResource(Res.string.settings_camera_cinematic_pacing_relaxed)),
            SelectOption(CinematicPacing.Normal, stringResource(Res.string.settings_camera_cinematic_pacing_normal)),
            SelectOption(
                CinematicPacing.Energetic,
                stringResource(Res.string.settings_camera_cinematic_pacing_energetic),
            ),
        ),
    )
}

/**
 * A slider row. The setting is saved when the thumb is released, not on every step of the drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FieldOfViewSelect(settings: State<AppSettings>, model: SettingsModel) {
    val saved = settings.value.cameraSettings.defaultFieldOfView

    @Composable
    fun FovSlider(modifier: Modifier) {
        var sliderValue by remember(saved) { mutableFloatStateOf(saved) }
        val interactionSource = remember { MutableInteractionSource() }
        val isDragged by interactionSource.collectIsDraggedAsState()
        val gap = with(LocalDensity.current) { 8.dp.roundToPx() }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            modifier = modifier,
            valueRange = 30f..90f,
            steps = 11,
            onValueChangeFinished = { model.setDefaultFieldOfView(sliderValue) },
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(interactionSource, thumbSize = DpSize(4.dp, 44.dp))
                if (isDragged) {
                    Popup(
                        popupPositionProvider = object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize,
                            ) = IntOffset(
                                anchorBounds.center.x - popupContentSize.width / 2,
                                anchorBounds.top - popupContentSize.height - gap,
                            )
                        },
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(width = 56.dp, height = 44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${sliderValue.roundToInt()}°",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    thumbTrackGapSize = 6.dp,
                    trackInsideCornerSize = 2.dp,
                )
            },
        )
    }

    SettingsRow(
        title = stringResource(Res.string.settings_camera_field_of_view_title),
        description = stringResource(Res.string.settings_camera_field_of_view_label_prefix),
        icon = Res.drawable.camera_video,
        trailing = {
            Text(
                text = "${saved.roundToInt()}°",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        below = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.zoom_in),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FovSlider(Modifier.weight(1f))
                Icon(
                    painter = painterResource(Res.drawable.zoom_out),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
internal fun SynthesizerReverbSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_playback_synthesizer_reverb),
        description = stringResource(Res.string.settings_playback_synthesizer_reverb_description),
        icon = Res.drawable.surround_sound,
        checked = settings.value.playbackSettings.synthesizerSettings.isUseReverb,
        onCheckedChange = model::setUseReverb,
    )
}

@Composable
internal fun SynthesizerChorusSelect(settings: State<AppSettings>, model: SettingsModel) {
    SettingsSwitchRow(
        title = stringResource(Res.string.settings_playback_synthesizer_chorus),
        description = stringResource(Res.string.settings_playback_synthesizer_chorus_description),
        icon = Res.drawable.graphic_eq,
        checked = settings.value.playbackSettings.synthesizerSettings.isUseChorus,
        onCheckedChange = model::setUseChorus,
    )
}
