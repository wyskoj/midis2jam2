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

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.add_circle
import midis2jam2.app.generated.resources.android
import midis2jam2.app.generated.resources.audio_file
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.hand_gesture_off
import midis2jam2.app.generated.resources.language
import midis2jam2.app.generated.resources.settings_camera
import midis2jam2.app.generated.resources.settings_controls
import midis2jam2.app.generated.resources.settings_controls_disable_touch
import midis2jam2.app.generated.resources.settings_controls_disable_touch_description
import midis2jam2.app.generated.resources.settings_general
import midis2jam2.app.generated.resources.settings_general_locale
import midis2jam2.app.generated.resources.settings_graphics
import midis2jam2.app.generated.resources.settings_instruments
import midis2jam2.app.generated.resources.settings_on_screen_elements
import midis2jam2.app.generated.resources.settings_playback_soundbanks
import midis2jam2.app.generated.resources.settings_playback_soundbanks_add
import midis2jam2.app.generated.resources.settings_playback_soundbanks_description
import midis2jam2.app.generated.resources.settings_playback_soundbanks_none_loaded
import midis2jam2.app.generated.resources.settings_playback_synthesizer
import midis2jam2.app.generated.resources.warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.domain.LocaleHelper
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.copyBytesToInternalStorage
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.ui.common.component.CategoryHeader
import org.wysko.midis2jam2.ui.common.component.SelectOption
import org.wysko.midis2jam2.ui.common.component.SelectRow
import org.wysko.midis2jam2.ui.common.component.SwitchRow
import org.wysko.midis2jam2.ui.common.component.UnitRow
import org.wysko.midis2jam2.ui.common.component.WarningAmber
import java.io.File
import java.util.Locale

@Composable
internal actual fun SettingsScreenOverlay(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    SettingsSoundbanksSheet(
        settings = settings,
        model = model,
        screenModel = screenModel,
    )
}

internal actual fun LazyListScope.SettingsScreenContent(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    stickyHeader {
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
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_graphics))
    }
    item {
        ShadowsBooleanSelect(settings, model)
    }
    item {
        BackgroundSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_on_screen_elements))
    }
    LyricsSelect(settings, model)
    item {
        HudBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_instruments))
    }
    item {
        AlwaysShowInstrumentsBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_controls))
    }
    item {
        DisableTouchInputBooleanSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_playback_synthesizer))
    }
    item {
        SoundbanksSelect(settings, model, screenModel)
    }
    item {
        SynthesizerReverbSelect(settings, model)
    }
    item {
        SynthesizerChorusSelect(settings, model)
    }
    stickyHeader {
        CategoryHeader(stringResource(Res.string.settings_camera))
    }
    item {
        StartAutocamWithSongBooleanSelect(settings, model)
    }
    item {
        IsClassicAutoCamBooleanSelect(settings, model)
    }
    item {
        FieldOfViewSelect(settings, model)
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}

internal actual val deviceThemeIcon: DrawableResource
    get() = Res.drawable.android

@Composable
internal actual fun LocaleSelect(
    selectedLocale: String,
    onSelectLocale: (String) -> Unit,
    availableLocales: List<String>,
) {
    val systemInteraction = koinInject<SystemInteractionService>()
    val localConfig = LocalConfiguration.current
    val context = LocalContext.current

    when (CompatLibrary.useLegacyLanguageSelect) {
        true -> {
            val options = availableLocales.map {
                val locale = Locale(it)
                SelectOption(
                    value = it,
                    title = locale.displayName,
                )
            }
            // Forces recomposition on locale change (12-)
            val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
            SelectRow(
                selectedLocale,
                {
                    onSelectLocale(it)
                    LocaleHelper.updateLocale(context, it)
                },
                options = options,
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                icon = Res.drawable.language,
            )
        }

        false -> {
            UnitRow(
                title = { Text(stringResource(Res.string.settings_general_locale)) },
                label = { Text(systemInteraction.getLocale().displayLanguage) },
                icon = Res.drawable.language,
            ) {
                systemInteraction.openSystemLanguageSettings()
            }
        }
    }
}

@Composable
private fun DisableTouchInputBooleanSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
) {
    SwitchRow(
        settings.value.controlsSettings.isDisableTouchInput,
        model::setDisableTouchInput,
        title = { Text(stringResource(Res.string.settings_controls_disable_touch)) },
        label = { Text(stringResource(Res.string.settings_controls_disable_touch_description)) },
        icon = Res.drawable.hand_gesture_off,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundbanksSelect(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    UnitRow(
        title = { Text(stringResource(Res.string.settings_playback_soundbanks)) },
        label = { Text(stringResource(Res.string.settings_playback_soundbanks_description)) },
        icon = Res.drawable.audio_file,
    ) {
        screenModel.requestOpenSoundbanks()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSoundbanksSheet(
    settings: State<AppSettings>,
    model: SettingsModel,
    screenModel: SettingsScreenModel,
) {
    val soundbanks = settings.value.playbackSettings.soundbanksSettings.soundbanks
    val context = LocalContext.current
    var isImportingSoundbanks by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(
        type = PickerType.File(listOf("sf2")),
        mode = PickerMode.Multiple(),
        title = stringResource(Res.string.settings_playback_soundbanks_add),
    ) { files ->
        files?.let { platformFiles ->
            scope.launch {
                isImportingSoundbanks = true
                val paths = platformFiles.mapNotNull { pf ->
                    runCatching {
                        val bytes = pf.readBytes()
                        context.copyBytesToInternalStorage(pf.name, bytes)
                    }.getOrNull()
                }
                if (paths.isNotEmpty()) {
                    model.addSoundbanks(paths)
                }
                isImportingSoundbanks = false
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isShowSheet by remember { mutableStateOf(false) }
    val openSoundbanksRequest by screenModel.openSoundbanksRequest.collectAsState()

    LaunchedEffect(openSoundbanksRequest) {
        if (openSoundbanksRequest) {
            isShowSheet = true
            screenModel.consumeOpenSoundbanksRequest()
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
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { picker.launch() },
                    enabled = !isImportingSoundbanks,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    if (isImportingSoundbanks) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.add_circle),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.settings_playback_soundbanks_add),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (soundbanks.isEmpty()) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            ) {
                                Text(
                                    stringResource(Res.string.settings_playback_soundbanks_none_loaded),
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    items(soundbanks) {
                        val isMissing = !File(it).exists()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) {
                            if (isMissing) {
                                Icon(
                                    painterResource(Res.drawable.warning),
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.padding(end = 4.dp).size(18.dp),
                                )
                            }
                            Text(
                                text = File(it).name,
                                modifier = Modifier.weight(1f, true),
                            )
                            IconButton(onClick = { model.removeSoundbank(it) }) {
                                Icon(painterResource(Res.drawable.close), null)
                            }
                        }
                    }
                }
            }
        }
    }
}
