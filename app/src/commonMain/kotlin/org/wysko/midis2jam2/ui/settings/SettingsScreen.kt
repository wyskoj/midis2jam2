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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.Color
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.CubeMap
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType.Default
import org.wysko.midis2jam2.domain.settings.AppTheme
import org.wysko.midis2jam2.ui.BasicDeviceScaffold
import org.wysko.midis2jam2.ui.common.component.*
import kotlin.math.roundToInt

object SettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Suppress("DuplicatedCode")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinScreenModel<SettingsModel>()
        val screenModel = koinScreenModel<SettingsScreenModel>()
        val settings = model.appSettings.collectAsState()
        val systemInteractionService = koinInject<SystemInteractionService>()

        BasicDeviceScaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.tab_settings)) },
                    actions = {
                        IconButton(
                            onClick = {
                                systemInteractionService.openOnlineDocumentation()
                            },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.help),
                                contentDescription = stringResource(Res.string.help)
                            )
                        }
                    }
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsScreenContent(settings, model, screenModel)
            }
        }
    }
}

internal expect fun LazyListScope.SettingsScreenContent(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
)

internal expect val deviceThemeIcon: DrawableResource

@Composable
internal fun SynthesizerReverbSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        checked = settings.value.playbackSettings.synthesizerSettings.isUseReverb,
        onCheckedChange = model::setUseReverb,
        title = { Text(stringResource(Res.string.settings_playback_synthesizer_reverb)) },
        label = { Text(stringResource(Res.string.settings_playback_synthesizer_reverb_description)) },
        icon = Res.drawable.surround_sound,
    )
}

@Composable
internal fun SynthesizerChorusSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        checked = settings.value.playbackSettings.synthesizerSettings.isUseChorus,
        onCheckedChange = model::setUseChorus,
        title = { Text(stringResource(Res.string.settings_playback_synthesizer_chorus)) },
        label = { Text(stringResource(Res.string.settings_playback_synthesizer_chorus_description)) },
        icon = Res.drawable.graphic_eq,
    )
}

@Composable
internal fun IsClassicAutoCamBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.cameraSettings.isClassicAutoCam,
        model::setClassicAutoCam,
        title = { Text(stringResource(Res.string.settings_camera_classic_autocam)) },
        label = { Text(stringResource(Res.string.settings_camera_classic_autocam_description)) },
        icon = Res.drawable.camera_video,
    )
}

@Composable
internal fun StartAutocamWithSongBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.cameraSettings.isStartAutocamWithSong,
        model::setStartAutocamWithSong,
        title = { Text(stringResource(Res.string.settings_camera_start_autocam_with_song)) },
        label = {
            Text(
                stringResource(Res.string.settings_camera_start_autocam_with_song_description)
            )
        },
        icon = Res.drawable.motion_photos_auto,
    )
}

@Composable
internal fun AlwaysShowInstrumentsBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.instrumentSettings.isAlwaysShowInstruments,
        model::setAlwaysShowInstruments,
        title = { Text(stringResource(Res.string.settings_instruments_always_show_instruments)) },
        label = {
            Text(stringResource(Res.string.settings_instruments_always_show_instruments_description))
        },
        icon = Res.drawable.keep,
    )
}

@Composable
internal fun HudBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.onScreenElementsSettings.isShowHeadsUpDisplay,
        model::setShowHeadsUpDisplay,
        title = { Text(stringResource(Res.string.settings_onscreenelements_hud)) },
        label = { Text(stringResource(Res.string.settings_onscreenelements_hud_description)) },
        icon = Res.drawable.browse_activity,
    )
}

internal fun LazyListScope.LyricsSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    item {
        SwitchRow(
            settings.value.onScreenElementsSettings.lyricsSettings.isShowLyrics,
            model::setShowLyrics,
            title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics)) },
            label = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_description)) },
            icon = Res.drawable.lyrics,
        )
    }
    item {
        val scaleOptions = listOf(
            SelectOption(
                value = 0.5,
                title = stringResource(Res.string.settings_onscreenelements_lyrics_size_smaller)
            ),
            SelectOption(
                value = 1.0,
                title = stringResource(Res.string.settings_onscreenelements_lyrics_size_small)
            ),
            SelectOption(
                value = 1.5,
                title = stringResource(Res.string.settings_onscreenelements_lyrics_size_default)
            ),
            SelectOption(
                value = 2.0,
                title = stringResource(Res.string.settings_onscreenelements_lyrics_size_large)
            ),
            SelectOption(
                value = 2.5,
                title = stringResource(Res.string.settings_onscreenelements_lyrics_size_larger)
            ),
        )
        AnimatedVisibility(
            visible = settings.value.onScreenElementsSettings.lyricsSettings.isShowLyrics,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SelectRow(
                option = settings.value.onScreenElementsSettings.lyricsSettings.lyricsSize,
                model::setLyricsSize,
                options = scaleOptions,
                title = { Text(stringResource(Res.string.settings_onscreenelements_lyrics_size)) },
                icon = Res.drawable.format_size,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
    backgroundWarning: BackgroundWarning? = null,
) {
    var showColorSelectModal by remember { mutableStateOf(false) }
    val colorSelectSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBackgroundWarningDialog by remember { mutableStateOf(false) }

    var showCubeMapSelectModal by remember { mutableStateOf(false) }
    val cubeMapSelectSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val color = settings.value.backgroundSettings.color

    val backgroundImagesOptions = listOf(
        SelectOption(
            value = CubeMap,
            title = stringResource(Res.string.settings_background_type_cubemap),
            label = stringResource(Res.string.settings_background_type_cubemap_description),
            icon = Res.drawable.image,
        ),
    )

    SelectRow(
        settings.value.backgroundSettings.type,
        {
            model.setBackgroundType(it)
            when (it) {
                CubeMap -> showCubeMapSelectModal = true
                Color -> showColorSelectModal = true
                else -> Unit
            }
        },
        options = listOf(
            SelectOption(
                value = Default,
                title = stringResource(Res.string.settings_background_type_default),
                label = stringResource(Res.string.settings_background_type_default_description),
                icon = Res.drawable.wallpaper,
            ),
            SelectOption(
                value = Color,
                title = stringResource(Res.string.settings_background_type_color),
                label = stringResource(Res.string.settings_background_type_color_description),
                icon = Res.drawable.palette,
            ),
        ) + if (Platform.current() == Platform.Desktop) backgroundImagesOptions else emptyList(),
        title = { Text(stringResource(Res.string.settings_background_type)) },
        trailingIcon = {
            if (settings.value.backgroundSettings.type == Color) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(color),
                ) {}
            }
            if (backgroundWarning != null) {
                IconButton(onClick = { showBackgroundWarningDialog = true }) {
                    Icon(
                        painterResource(Res.drawable.warning),
                        contentDescription = stringResource(Res.string.background_warning_settings_badge),
                        tint = WarningAmber,
                    )
                }
            }
        },
        description = stringResource(Res.string.settings_background_description)
    )

    if (showBackgroundWarningDialog && backgroundWarning != null) {
        val textButtonColors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
        val title = backgroundWarningTitle(backgroundWarning)
        val message = backgroundWarningMessage(backgroundWarning)
        AlertDialog(
            onDismissRequest = { showBackgroundWarningDialog = false },
            title = { Text(title) },
            icon = {
                Icon(
                    painterResource(Res.drawable.warning),
                    contentDescription = title,
                    tint = WarningAmber,
                )
            },
            text = { Text(message) },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showBackgroundWarningDialog = false },
                    colors = textButtonColors,
                ) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showColorSelectModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showColorSelectModal = false
            },
            sheetState = colorSelectSheetState,
        ) {
            var formColor by remember { mutableIntStateOf(color) }
            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                ColorPicker(
                    color = formColor,
                    setColor = model::setBackgroundColor,
                )
            }
        }
    }

    if (showCubeMapSelectModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showCubeMapSelectModal = false
            },
            sheetState = cubeMapSelectSheetState,
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                CubeMapImageSelect(
                    settings = settings.value,
                    model = model,
                )
            }
        }
    }
}

@Composable
internal fun ShadowsBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.graphicsSettings.shadowsSettings.isUseShadows,
        model::setUseShadows,
        title = { Text(stringResource(Res.string.settings_graphics_shadows_description_a)) },
        label = { Text(stringResource(Res.string.settings_graphics_shadows_description_hint_a)) },
        icon = Res.drawable.tonality,
    )
}

@Composable
internal fun ThemeSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SelectRow(
        settings.value.generalSettings.theme,
        model::setAppTheme,
        options = listOf(
            SelectOption(
                value = AppTheme.LIGHT,
                title = stringResource(Res.string.settings_general_theme_light),
                label = null,
                icon = Res.drawable.light_mode,
            ),
            SelectOption(
                value = AppTheme.DARK,
                title = stringResource(Res.string.settings_general_theme_dark),
                label = null,
                icon = Res.drawable.dark_mode,
            ),
            SelectOption(
                value = AppTheme.SYSTEM_DEFAULT,
                title = stringResource(Res.string.settings_general_theme_system),
                label = null,
                icon = deviceThemeIcon,
            ),
        ),
        title = { Text(stringResource(Res.string.settings_general_theme)) },
    )
}

@Composable
internal expect fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FieldOfViewSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isShowSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    @Composable
    fun formatFov(degrees: Float): String {
        return stringResource(Res.string.settings_camera_field_of_view_degrees, degrees.roundToInt())
    }

    UnitRow(
        title = { Text(stringResource(Res.string.settings_camera_field_of_view_title)) },
        label = {
            val prefix = stringResource(Res.string.settings_camera_field_of_view_label_prefix)
            val current = formatFov(settings.value.cameraSettings.defaultFieldOfView)
            Text("$prefix ∙ $current")
        },
        icon = Res.drawable.zoom_in,
    ) {
        isShowSheet = true
        sliderPosition = settings.value.cameraSettings.defaultFieldOfView
    }

    if (isShowSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    model.setDefaultFieldOfView(sliderPosition)
                    sheetState.hide()
                    isShowSheet = false
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(formatFov(sliderPosition))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(Res.drawable.zoom_in), null)
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                        },
                        valueRange = 30f..90f,
                        modifier = Modifier.weight(1f),
                        steps = 11,
                    )
                    Icon(painterResource(Res.drawable.zoom_out), null)
                }
            }
        }
    }
}