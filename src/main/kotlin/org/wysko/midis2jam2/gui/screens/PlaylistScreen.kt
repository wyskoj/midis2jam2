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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import midis2jam2.generated.resources.*
import midis2jam2.generated.resources.Res
import midis2jam2.generated.resources.file_open
import midis2jam2.generated.resources.file_save
import midis2jam2.generated.resources.help
import midis2jam2.generated.resources.playlist_add
import midis2jam2.generated.resources.playlist_play
import midis2jam2.generated.resources.playlist_remove
import org.jetbrains.compose.resources.painterResource
import org.wysko.midis2jam2.gui.util.openHelp
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.PlaylistViewModel
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File

@Composable
fun PlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    isLockPlayButton: Boolean,
    onPlayPlaylist: () -> Unit,
) {
    val isShuffle by playlistViewModel.isShuffle.collectAsState()
    val playlist by playlistViewModel.playlist.collectAsState()

    var isPlaylistDirty by remember { mutableStateOf(false) }

    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
    var isConfirmClearDialogOpen by remember { mutableStateOf(false) }
    var isPlaylistLoadDialogOpen by remember { mutableStateOf(false) }
    var isPlaylistLoadWarningsDialogOpen by remember { mutableStateOf(false) }
    var missingFiles by remember { mutableStateOf(listOf<String>()) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val midiFileLauncher = rememberFilePickerLauncher(
        type = PickerType.File(MIDI_FILE_EXTENSIONS),
        mode = PickerMode.Multiple(),
        title = "Select MIDI files",
    ) { files ->
        if (files != null) {
            playlistViewModel.setPlaylist((playlist + files.map { it.file }).distinct())
            isPlaylistDirty = true
        }
    }

    val playlistFileLoadLauncher = rememberFilePickerLauncher(
        type = PickerType.File(listOf("txt")),
        mode = PickerMode.Single,
        title = "Select playlist",
    ) { file ->
        if (file != null) {
            val files = file.file.readText().split("\n").map { File(it.trim()) }
            val filesIsFound = files.associateWith { it.exists() }
            isPlaylistDirty = false
            if (filesIsFound.values.all { it }) {
                playlistViewModel.setPlaylist(files.filter { filesIsFound[it] == true })
            } else {
                missingFiles = filesIsFound.filterValues { !it }.keys.map { it.absolutePath }
                playlistViewModel.setPlaylist(files.filter { filesIsFound[it] == true })
                isPlaylistLoadWarningsDialogOpen = true
            }
        }
    }

    val playlistFileSaveLauncher = rememberFileSaverLauncher {
        isPlaylistDirty = false
        if (it != null) {
            scope.launch { snackbarHostState.showSnackbar(I18n["playlist_save_confirmation"].value) }
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        playlistViewModel.setPlaylist(playlist.toMutableList().apply {
            add(to.index, removeAt(from.index))
            isPlaylistDirty = true
        })
    }


    fun implClearPlaylist() {
        playlistViewModel.clearPlaylist()
        isPlaylistDirty = false
    }

    fun implLoadPlaylist() {
        playlistFileLoadLauncher.launch()
    }

    fun implSavePlaylist() {
        val textContents = playlist.joinToString("\n") { it.absolutePath }
        playlistFileSaveLauncher.launch(textContents.toByteArray(), "playlist", "txt")
        isPlaylistDirty = false
    }

    fun implRemoveItem(index: Int) {
        playlistViewModel.setPlaylist(playlist.toMutableList().apply {
            removeAt(index)
            isPlaylistDirty = true
        })
    }

    fun onLoadPlaylist() {
        if (isPlaylistDirty && playlist.isNotEmpty()) {
            isPlaylistLoadDialogOpen = true
        } else {
            playlistFileLoadLauncher.launch()
        }
    }

    fun onClearPlaylist() {
        if (isPlaylistDirty && playlist.isNotEmpty()) {
            isConfirmClearDialogOpen = true
        } else {
            implClearPlaylist()
        }
    }

    fun onSavePlaylist() = implSavePlaylist()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        when {
            isConfirmClearDialogOpen -> ConfirmPlaylistClearAlertDialog({
                isConfirmClearDialogOpen = false
                implClearPlaylist()
            }, {
                isConfirmClearDialogOpen = false
            })

            isPlaylistLoadDialogOpen -> PlaylistLoadAlertDialog({
                isPlaylistLoadDialogOpen = false
                implLoadPlaylist()
            }, {
                isPlaylistLoadDialogOpen = false
            })

            isPlaylistLoadWarningsDialogOpen -> PlaylistLoadWarningsDialog(missingFiles) {
                isPlaylistLoadWarningsDialogOpen = false
            }
        }
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(I18n["playlist"].value, style = typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Button(onClick = onPlayPlaylist, enabled = playlist.isNotEmpty() && !isLockPlayButton) {
                    Icon(painterResource(Res.drawable.playlist_play), "")
                    Spacer(Modifier.width(8.dp))
                    Text(I18n["play"].value)
                }
                IconButton(
                    onClick = { playlistViewModel.setIsShuffle(!isShuffle) },
                ) {
                    Icon(
                        painterResource(if (isShuffle) Res.drawable.shuffle_on else Res.drawable.shuffle),
                        "",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier)
                FilledTonalButton(onClick = {
                    midiFileLauncher.launch()
                }) {
                    Icon(painterResource(Res.drawable.playlist_add), "")
                    Spacer(Modifier.width(8.dp))
                    Text(I18n["playlist_add"].value)
                }
                Box {
                    IconButton(onClick = {
                        isDropdownMenuExpanded = true
                    }) {
                        Icon(Icons.Default.MoreVert, "")
                    }
                    DropdownMenu(
                        expanded = isDropdownMenuExpanded,
                        onDismissRequest = { isDropdownMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {

                        DropdownMenuItem(
                            text = { Text(I18n["playlist_load"].value) },
                            leadingIcon = { Icon(painterResource(Res.drawable.file_open), "") },
                            onClick = {
                                isDropdownMenuExpanded = false
                                onLoadPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(I18n["playlist_save"].value) },
                            leadingIcon = { Icon(painterResource(Res.drawable.file_save), "") },
                            onClick = {
                                isDropdownMenuExpanded = false
                                onSavePlaylist()
                            },
                            enabled = playlist.isNotEmpty()
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(I18n["playlist_clear"].value) },
                            leadingIcon = { Icon(painterResource(Res.drawable.playlist_remove), "") },
                            onClick = {
                                isDropdownMenuExpanded = false
                                onClearPlaylist()
                            },
                            enabled = playlist.isNotEmpty()
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(I18n["help"].value) },
                            leadingIcon = { Icon(painterResource(Res.drawable.help), "") },
                            onClick = {
                                isDropdownMenuExpanded = false
                                openHelp("screens", "playlist")
                            }
                        )
                    }
                }
            }
            Column {
                when (playlist.size) {
                    0 -> Text(I18n["playlist_no_files"].value, fontStyle = FontStyle.Italic)
                    else -> LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(playlist, key = { it }) {
                            val index = playlist.indexOf(it)
                            ReorderableItem(reorderableLazyListState, key = it) { isDragging ->
                                Surface(
                                    Modifier.height(64.dp).fillMaxWidth(),
                                    color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = when {
                                        playlist.size == 1 -> MaterialTheme.shapes.medium
                                        index == 0 -> MaterialTheme.shapes.medium.copy(
                                            bottomStart = CornerSize(0.dp),
                                            bottomEnd = CornerSize(0.dp)
                                        )

                                        index == playlist.indices.last -> MaterialTheme.shapes.medium.copy(
                                            topStart = CornerSize(0.dp),
                                            topEnd = CornerSize(0.dp)
                                        )

                                        else -> MaterialTheme.shapes.medium.copy(
                                            topStart = CornerSize(0.dp),
                                            topEnd = CornerSize(0.dp),
                                            bottomStart = CornerSize(0.dp),
                                            bottomEnd = CornerSize(0.dp)
                                        )
                                    }
                                ) {
                                    Row(
                                        Modifier.fillMaxSize().padding(horizontal = 16.dp).draggableHandle(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            (index + 1).toString(),
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Column{
                                            Text(it.name, fontWeight = FontWeight.Medium)
                                            Text(text = it.parent, style = typography.labelSmall)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            implRemoveItem(index)
                                        }) {
                                            Icon(Icons.Default.Delete, "")
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistLoadWarningsDialog(
    missingFiles: List<String>,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n["playlist_some_files_missing"].value) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(I18n["playlist_some_files_missing_help"].value)
                HorizontalDivider()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(missingFiles) {
                        Text(it)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text("Okay")
            }
        }
    )
}

@Composable
fun PlaylistLoadAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n["playlist_load_prompt"].value) },
        icon = { Icon(painterResource(Res.drawable.file_open), "", tint = MaterialTheme.colorScheme.primary) },
        text = { Text(I18n["playlist_load_warning"].value) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = textButtonColors) {
                Text(I18n["playlist_load"].value)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text(I18n["cancel"].value)
            }
        }
    )
}

@Composable
fun ConfirmPlaylistClearAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n["playlist_clear_prompt"].value) },
        icon = { Icon(painterResource(Res.drawable.playlist_remove), "", tint = MaterialTheme.colorScheme.primary) },
        text = { Text(I18n["playlist_clear_warning"].value) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = textButtonColors) {
                Text(I18n["playlist_clear"].value)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text(I18n["cancel"].value)
            }
        }
    )
}