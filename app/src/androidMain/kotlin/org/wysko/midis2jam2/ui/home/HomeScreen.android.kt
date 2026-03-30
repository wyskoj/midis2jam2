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

@file:OptIn(ExperimentalMaterial3Api::class)

package org.wysko.midis2jam2.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.add_circle
import midis2jam2.app.generated.resources.audio_file
import midis2jam2.app.generated.resources.background_cubemap_warning_continue
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.ok
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.play_midi_file
import midis2jam2.app.generated.resources.settings_fill
import midis2jam2.app.generated.resources.soundbank_default
import midis2jam2.app.generated.resources.soundbank_missing_warning_message
import midis2jam2.app.generated.resources.soundbank_missing_warning_title
import midis2jam2.app.generated.resources.settings_playback_soundbanks_add
import midis2jam2.app.generated.resources.soundbank_manage
import midis2jam2.app.generated.resources.soundbank_invalid_file_message
import midis2jam2.app.generated.resources.soundbank_invalid_file_title
import midis2jam2.app.generated.resources.soundbank_selector_title
import midis2jam2.app.generated.resources.warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.copyBytesToInternalStorage
import org.wysko.midis2jam2.ui.AppNavigationBar
import org.wysko.midis2jam2.ui.common.component.Midis2jam2Logo
import org.wysko.midis2jam2.ui.common.component.WarningAmber
import org.wysko.midis2jam2.ui.home.log.LogScreenButton
import org.wysko.midis2jam2.ui.settings.SettingsModel
import org.wysko.midis2jam2.ui.settings.SettingsScreenModel
import org.wysko.midis2jam2.ui.settings.SettingsTab
import org.wysko.midis2jam2.ui.tutorial.TutorialScreen
import java.io.File

@Composable
internal actual fun HomeScreenLayout() {
    val model = koinInject<HomeScreenModel>()
    val navigator = LocalNavigator.currentOrThrow

    LaunchedEffect(Unit) {
        model.loadState()
    }

    // When a soundbank is removed from settings, clear the selection on the home tab
    val soundbanks = model.soundbanks.collectAsState(initial = emptyList())
    LaunchedEffect(soundbanks.value) {
        val currentPath = model.selectedSoundbank.value?.uri?.path ?: return@LaunchedEffect
        if (soundbanks.value.none { it.uri?.path == currentPath }) {
            model.setSelectedSoundbank(null)
        }
    }

    Scaffold(
        bottomBar = { AppNavigationBar() },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
        ) {
            LogScreenButton(
                navigator, Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {
                Midis2jam2Logo()
                SelectAndPlayMidiFile(model)
                SoundbankSelector(model)
            }
        }
    }
}

@Composable
private fun SoundbankSelector(model: HomeScreenModel) {
    val settingsModel = koinInject<SettingsModel>()
    val settingsScreenModel = koinInject<SettingsScreenModel>()
    val tabNavigator = LocalTabNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedSoundbank = model.selectedSoundbank.collectAsState()
    val soundbanks = model.soundbanks.collectAsState(initial = emptyList())
    val selectedSoundbankName = selectedSoundbank.value?.name ?: stringResource(Res.string.soundbank_default)
    val isMissing = remember(selectedSoundbank.value) {
        val path = selectedSoundbank.value?.uri?.path
        path != null && !File(path).exists()
    }
    val selectorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isShowSelectorSheet by remember { mutableStateOf(false) }
    var isImportingSoundbanks by remember { mutableStateOf(false) }
    var showInvalidSoundbankDialog by remember { mutableStateOf(false) }
    val addSoundbanksLauncher = rememberFilePickerLauncher(
        type = PickerType.File(listOf("sf2", "SF2")),
        mode = PickerMode.Multiple(),
        title = stringResource(Res.string.settings_playback_soundbanks_add),
    ) { files ->
        files?.let { platformFiles ->
            scope.launch {
                isImportingSoundbanks = true
                val importedPaths = platformFiles.mapNotNull { pf ->
                    runCatching {
                        if (!pf.name.endsWith(".sf2", ignoreCase = true)) {
                            null
                        } else {
                            val bytes = pf.readBytes()
                            context.copyBytesToInternalStorage(pf.name, bytes)
                        }
                    }.getOrNull()
                }

                if (importedPaths.isNotEmpty()) {
                    settingsModel.addSoundbanks(importedPaths)
                    val mostRecentlyImported = importedPaths.last()
                    val importedFile = File(mostRecentlyImported)
                    model.setSelectedSoundbank(
                        PlatformFile(
                            android.net.Uri.fromFile(importedFile),
                            context,
                        )
                    )
                }

                if (platformFiles.isNotEmpty() && importedPaths.isEmpty()) {
                    showInvalidSoundbankDialog = true
                }
                isImportingSoundbanks = false
            }
        }
    }

    AssistChip(
        onClick = { isShowSelectorSheet = true },
        label = { Text(selectedSoundbankName) },
        leadingIcon = {
            if (isImportingSoundbanks) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painterResource(if (isMissing) Res.drawable.warning else Res.drawable.audio_file),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isMissing) WarningAmber else androidx.compose.ui.graphics.Color.Unspecified,
                )
            }
        },
    )

    if (isShowSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    selectorSheetState.hide()
                    isShowSelectorSheet = false
                }
            },
            sheetState = selectorSheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.soundbank_selector_title),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = { addSoundbanksLauncher.launch() },
                    enabled = !isImportingSoundbanks,
                    modifier = Modifier.fillMaxWidth(),
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
                HorizontalDivider()
                TextButton(
                    onClick = {
                        settingsScreenModel.requestOpenSoundbanks()
                        scope.launch {
                            selectorSheetState.hide()
                            isShowSelectorSheet = false
                        }
                        tabNavigator.current = SettingsTab
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.settings_fill),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.soundbank_manage),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                ) {
                    item {
                        val isDefaultSelected = selectedSoundbank.value == null
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isDefaultSelected,
                                    onClick = {
                                        model.setSelectedSoundbank(null)
                                        scope.launch {
                                            selectorSheetState.hide()
                                            isShowSelectorSheet = false
                                        }
                                    },
                                    role = Role.RadioButton,
                                ),
                        ) {
                            RadioButton(
                                selected = isDefaultSelected,
                                onClick = {
                                    model.setSelectedSoundbank(null)
                                    scope.launch {
                                        selectorSheetState.hide()
                                        isShowSelectorSheet = false
                                    }
                                },
                            )
                            Text(stringResource(Res.string.soundbank_default))
                        }
                    }
                    items(soundbanks.value) { soundbank ->
                        val isSelected = selectedSoundbank.value?.uri?.path == soundbank.uri?.path
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        model.setSelectedSoundbank(soundbank)
                                        scope.launch {
                                            selectorSheetState.hide()
                                            isShowSelectorSheet = false
                                        }
                                    },
                                    role = Role.RadioButton,
                                ),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    model.setSelectedSoundbank(soundbank)
                                    scope.launch {
                                        selectorSheetState.hide()
                                        isShowSelectorSheet = false
                                    }
                                },
                            )
                            Text(soundbank.name)
                        }
                    }
                }
            }
        }
    }

    if (showInvalidSoundbankDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidSoundbankDialog = false },
            title = { Text(stringResource(Res.string.soundbank_invalid_file_title)) },
            text = { Text(stringResource(Res.string.soundbank_invalid_file_message)) },
            confirmButton = {
                TextButton(onClick = { showInvalidSoundbankDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }
}

@Composable
private fun SelectAndPlayMidiFile(
    model: HomeScreenModel,
) {
    val applicationService = koinInject<ApplicationService>()
    val navigator = LocalNavigator.currentOrThrow
    val selectedSoundbank = model.selectedSoundbank.collectAsState()

    var showMissingSoundbankDialog by remember { mutableStateOf(false) }

    val proceed = {
        if (applicationService.isFirstLaunch.value) {
            navigator.push(TutorialScreen)
        } else {
            model.startApplication()
        }
    }

    val picker = model.midiFilePicker {
        val currentSoundbank = selectedSoundbank.value
        val isMissing = currentSoundbank?.uri?.path?.let { !File(it).exists() } == true
        if (isMissing) {
            showMissingSoundbankDialog = true
        } else {
            proceed()
        }
    }

    Button(
        onClick = { picker.launch() },
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(painterResource(Res.drawable.play_arrow), "", modifier = Modifier.size(24.dp))
            Text(stringResource(Res.string.play_midi_file), fontSize = 16.sp)
        }
    }

    if (showMissingSoundbankDialog) {
        AlertDialog(
            onDismissRequest = { showMissingSoundbankDialog = false },
            icon = {
                Icon(
                    painterResource(Res.drawable.warning),
                    contentDescription = null,
                    tint = WarningAmber,
                )
            },
            title = { Text(stringResource(Res.string.soundbank_missing_warning_title)) },
            text = { Text(stringResource(Res.string.soundbank_missing_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showMissingSoundbankDialog = false
                    proceed()
                }) {
                    Text(stringResource(Res.string.background_cubemap_warning_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingSoundbankDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
