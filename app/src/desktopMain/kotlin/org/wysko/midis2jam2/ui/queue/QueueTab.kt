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

package org.wysko.midis2jam2.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.file_open
import midis2jam2.app.generated.resources.file_save
import midis2jam2.app.generated.resources.help
import midis2jam2.app.generated.resources.more_vert
import midis2jam2.app.generated.resources.music_note
import midis2jam2.app.generated.resources.play
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.playlist_add
import midis2jam2.app.generated.resources.playlist_remove
import midis2jam2.app.generated.resources.queue_add
import midis2jam2.app.generated.resources.queue_clear
import midis2jam2.app.generated.resources.queue_clear_prompt
import midis2jam2.app.generated.resources.queue_clear_warning
import midis2jam2.app.generated.resources.queue_empty
import midis2jam2.app.generated.resources.queue_load
import midis2jam2.app.generated.resources.queue_load_missing
import midis2jam2.app.generated.resources.queue_load_missing_help
import midis2jam2.app.generated.resources.queue_load_prompt
import midis2jam2.app.generated.resources.queue_load_warning
import midis2jam2.app.generated.resources.queue_save
import midis2jam2.app.generated.resources.queue_save_confirmation
import midis2jam2.app.generated.resources.queue_shuffle
import midis2jam2.app.generated.resources.shuffle
import midis2jam2.app.generated.resources.tab_queue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import org.wysko.midis2jam2.util.FileDragAndDrop
import org.wysko.midis2jam2.util.FilesDragAndDrop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

object QueueTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.tab_queue),
            icon = null,
        )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinScreenModel<QueueTabModel>()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val systemInteractionService = koinInject<SystemInteractionService>()
        val queueSaveConfirmation = stringResource(Res.string.queue_save_confirmation)

        // States
        val queue = model.queue.collectAsState()
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
            model.setQueue(
                queue.value.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                    model.setIsDirty(true)
                }
            )
        }
        var isConfirmClearDialogOpen by remember { mutableStateOf(false) }
        var isPlaylistLoadDialogOpen by remember { mutableStateOf(false) }
        var isPlaylistLoadWarningsDialogOpen by remember { mutableStateOf(false) }
        var missingFiles by remember { mutableStateOf(listOf<String>()) }
        val isPlayButtonEnabled = model.isPlayButtonEnabled.collectAsState(initial = false)

        // Pickers
        val midiFilePicker = model.midiFilePicker()
        val displayWarningDialog: (List<String>) -> Unit = {
            isPlaylistLoadWarningsDialogOpen = true
            missingFiles = it
        }
        val queueLoadPicker = model.queueLoadPicker(displayWarningDialog)
        val queueSavePicker = model.queueSavePicker {
            scope.launch {
                snackbarHostState.showSnackbar(queueSaveConfirmation)
                model.setIsDirty(false)
            }
        }

        fun openQueue() {
            if (model.isDirty.value && queue.value.isNotEmpty()) {
                isPlaylistLoadDialogOpen = true
            } else {
                queueLoadPicker.launch()
            }
        }

        fun saveQueue() {
            val textContents = queue.value.joinToString("\n") { it.file.absolutePath }
            queueSavePicker.launch(textContents.toByteArray(), "queue", "txt")
        }

        fun clearQueue() {
            if (model.isDirty.value && queue.value.isNotEmpty()) {
                isConfirmClearDialogOpen = true
            } else {
                model.clearQueue()
            }
        }

        val dragAndDropTarget = remember {
            FilesDragAndDrop {
                when {
                    it.size == 1 && it.first().extension.lowercase() == "txt" -> {
                        model.applyQueue(PlatformFile(it.first()), displayWarningDialog)
                    }

                    else -> {
                        val midis = it.filter { MIDI_FILE_EXTENSIONS.contains(it.extension.lowercase()) }
                        model.addToQueue(midis.map { PlatformFile(it) })
                    }
                }
            }
        }

        Scaffold(
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isPlayButtonEnabled.value,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                model.startApplication()
                            },
                            text = {
                                Text(stringResource(Res.string.play))
                            },
                            icon = {
                                Icon(painterResource(Res.drawable.play_arrow), null)
                            },
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.tab_queue)) },
                    actions = {
                        var isDropdownMenuExpanded by remember { mutableStateOf(false) }
                        Button(onClick = midiFilePicker::launch) {
                            Icon(painterResource(Res.drawable.playlist_add), stringResource(Res.string.queue_add))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.queue_add))
                        }
                        Spacer(Modifier.width(8.dp))
                        Box {
                            IconButton(onClick = {
                                isDropdownMenuExpanded = true
                            }) {
                                Icon(painterResource(Res.drawable.more_vert), "")
                            }
                            DropdownMenu(
                                expanded = isDropdownMenuExpanded,
                                onDismissRequest = { isDropdownMenuExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.queue_load)) },
                                    leadingIcon = { Icon(painterResource(Res.drawable.file_open), "") },
                                    onClick = {
                                        isDropdownMenuExpanded = false
                                        openQueue()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.queue_save)) },
                                    leadingIcon = { Icon(painterResource(Res.drawable.file_save), "") },
                                    onClick = {
                                        isDropdownMenuExpanded = false
                                        saveQueue()
                                    },
                                    enabled = queue.value.isNotEmpty()
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.queue_shuffle)) },
                                    leadingIcon = { Icon(painterResource(Res.drawable.shuffle), "") },
                                    onClick = {
                                        isDropdownMenuExpanded = false
                                        model.shuffleQueue()
                                        scope.launch {
                                            lazyListState.scrollToItem(0)
                                        }
                                    },
                                    enabled = queue.value.isNotEmpty()
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.queue_clear)) },
                                    leadingIcon = { Icon(painterResource(Res.drawable.playlist_remove), "") },
                                    onClick = {
                                        isDropdownMenuExpanded = false
                                        clearQueue()
                                    },
                                    enabled = queue.value.isNotEmpty()
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.help)) },
                                    leadingIcon = { Icon(painterResource(Res.drawable.help), "") },
                                    onClick = {
                                        isDropdownMenuExpanded = false
                                        systemInteractionService.openOnlineDocumentation()
                                    }
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget,
            ),
        ) { paddingValues ->

            when {
                isConfirmClearDialogOpen -> ConfirmPlaylistClearAlertDialog(
                    onConfirm = {
                        isConfirmClearDialogOpen = false
                        model.clearQueue()
                    },
                    onDismiss = {
                        isConfirmClearDialogOpen = false
                    }
                )

                isPlaylistLoadDialogOpen -> PlaylistLoadAlertDialog(
                    onConfirm = {
                        isPlaylistLoadDialogOpen = false
                        queueLoadPicker.launch()
                    },
                    onDismiss = {
                        isPlaylistLoadDialogOpen = false
                    }
                )

                isPlaylistLoadWarningsDialogOpen -> PlaylistLoadWarningsDialog(missingFiles) {
                    isPlaylistLoadWarningsDialogOpen = false
                }
            }

            when (queue.value.size) {
                0 -> {
                    Box(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(painterResource(Res.drawable.music_note), null, Modifier.size(64.dp))
                            Text(stringResource(Res.string.queue_empty))
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxHeight(),
                        state = lazyListState,
                    ) {
                        items(queue.value, key = { it }) {
                            val index = queue.value.indexOf(it)
                            ReorderableItem(reorderableLazyListState, key = it) { isDragging ->
                                Surface(
                                    Modifier.height(64.dp).fillMaxWidth(),
                                    color = if (isDragging) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = when {
                                        queue.value.size == 1 -> MaterialTheme.shapes.medium
                                        index == 0 -> MaterialTheme.shapes.medium.copy(
                                            bottomStart = CornerSize(0.dp),
                                            bottomEnd = CornerSize(0.dp)
                                        )

                                        index == queue.value.indices.last -> MaterialTheme.shapes.medium.copy(
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
                                        Column {
                                            Text(it.name, fontWeight = FontWeight.Medium)
                                            Text(text = it.file.parent, style = typography.labelSmall)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            model.removeAtIndex(index)
                                        }) {
                                            Icon(painterResource(Res.drawable.close), "")
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(88.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmPlaylistClearAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.queue_clear_prompt)) },
        icon = { Icon(painterResource(Res.drawable.playlist_remove), "", tint = MaterialTheme.colorScheme.primary) },
        text = { Text(stringResource(Res.string.queue_clear_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = textButtonColors) {
                Text(stringResource(Res.string.queue_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun PlaylistLoadAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.queue_load_prompt)) },
        icon = { Icon(painterResource(Res.drawable.file_open), "", tint = MaterialTheme.colorScheme.primary) },
        text = { Text(stringResource(Res.string.queue_load_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = textButtonColors) {
                Text(stringResource(Res.string.queue_load))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = textButtonColors) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun PlaylistLoadWarningsDialog(
    missingFiles: List<String>,
    onDismiss: () -> Unit,
) {
    val textButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.queue_load_missing)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(Res.string.queue_load_missing_help))
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
