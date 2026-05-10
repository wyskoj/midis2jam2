/*
 * Copyright (C) 2026 Jacob Wysko
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.add_circle
import midis2jam2.app.generated.resources.arrow_back
import midis2jam2.app.generated.resources.back
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.delete
import midis2jam2.app.generated.resources.settings_playback_soundbanks
import midis2jam2.app.generated.resources.settings_playback_soundbanks_delete
import midis2jam2.app.generated.resources.settings_playback_soundbanks_delete_message
import midis2jam2.app.generated.resources.settings_playback_soundbanks_delete_title
import midis2jam2.app.generated.resources.settings_playback_soundbanks_import
import midis2jam2.app.generated.resources.settings_playback_soundbanks_importing
import midis2jam2.app.generated.resources.settings_playback_soundbanks_none_loaded
import midis2jam2.app.generated.resources.warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.manager.AndroidSoundbanksManager
import org.wysko.midis2jam2.util.formatFileSize

object SoundbanksScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val soundbanksManager = koinInject<AndroidSoundbanksManager>()
        val soundbanks = soundbanksManager.soundbanks.collectAsState()
        val importJobs = soundbanksManager.jobs.collectAsState()
        val addSoundbanksLauncher = rememberFilePickerLauncher(
            type = FileKitType.File(listOf("sf2", "SF2")),
            mode = FileKitMode.Multiple()
        ) { files ->
            files?.let { platformFiles ->
                soundbanksManager.importSoundbanks(platformFiles)
            }
        }

        var openDeleteAlertDialog by remember { mutableStateOf(false) }
        var soundbankToDelete by remember { mutableStateOf("") }
        if (openDeleteAlertDialog) {
            AlertDialog(
                {
                    openDeleteAlertDialog = false
                },
                confirmButton = {
                    TextButton({
                        soundbanksManager.deleteSoundbank(soundbankToDelete)
                        openDeleteAlertDialog = false
                    }) {
                        Text(stringResource(Res.string.settings_playback_soundbanks_delete))
                    }
                },
                dismissButton = {
                    TextButton({ openDeleteAlertDialog = false }) { Text(stringResource(Res.string.cancel)) }
                },
                icon = {
                    Icon(painterResource(Res.drawable.delete), stringResource(Res.string.warning))
                },
                title = { Text(stringResource(Res.string.settings_playback_soundbanks_delete_title)) },
                text = {
                    Text(
                        stringResource(
                            Res.string.settings_playback_soundbanks_delete_message,
                            soundbankToDelete
                        )
                    )
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(Res.string.settings_playback_soundbanks))
                    },
                    modifier = Modifier,
                    navigationIcon = {
                        IconButton({
                            navigator.pop()
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_back),
                                contentDescription = stringResource(Res.string.back)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton({
                    addSoundbanksLauncher.launch()
                }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.add_circle),
                            stringResource(Res.string.settings_playback_soundbanks_import)
                        )
                        Text(stringResource(Res.string.settings_playback_soundbanks_import))
                    }
                }
            }
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(importJobs.value.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                stringResource(Res.string.settings_playback_soundbanks_importing),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(importJobs.value) { (_, name) ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text(name)
                                    }
                                }
                            }
                        }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (soundbanks.value.size) {
                        0 -> item {
                            Text(
                                text = stringResource(Res.string.settings_playback_soundbanks_none_loaded),
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .fillMaxWidth(),
                            )
                        }

                        else -> {
                            items(soundbanks.value.sortedBy { it.name }) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(it.name)
                                        Text(
                                            text = formatFileSize(it.size()),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    IconButton({
                                        soundbankToDelete = it.name
                                        openDeleteAlertDialog = true
                                    }) {
                                        Icon(
                                            painterResource(Res.drawable.delete),
                                            stringResource(Res.string.settings_playback_soundbanks_delete)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}