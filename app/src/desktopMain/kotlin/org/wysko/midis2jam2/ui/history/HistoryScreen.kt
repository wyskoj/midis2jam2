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

package org.wysko.midis2jam2.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.arrow_back
import midis2jam2.app.generated.resources.clear_all
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.help
import midis2jam2.app.generated.resources.history_clear_invalid_song
import midis2jam2.app.generated.resources.history_empty
import midis2jam2.app.generated.resources.history_file_missing_message
import midis2jam2.app.generated.resources.history_file_missing_title
import midis2jam2.app.generated.resources.history_unknown_song
import midis2jam2.app.generated.resources.more_vert
import midis2jam2.app.generated.resources.music_note
import midis2jam2.app.generated.resources.play
import midis2jam2.app.generated.resources.play_arrow
import midis2jam2.app.generated.resources.tab_history
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.PlaybackHistoryEntry
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import java.io.File
import java.text.DateFormat
import java.util.Date

object HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinInject<HistoryScreenModel>()
        val entries = model.historyEntries.collectAsState()
        val navigationModel = koinInject<NavigationModel>()
        val navigator = LocalNavigator.currentOrThrow
        val systemInteractionService = koinInject<SystemInteractionService>()

        var missingEntry by remember { mutableStateOf<PlaybackHistoryEntry?>(null) }

        fun replay(entry: PlaybackHistoryEntry) {
            val file = File(entry.filePath)
            if (!file.exists()) {
                missingEntry = entry
                return
            }
            navigationModel.setApplyHomeScreenMidiFile(file)
            navigator.pop()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.tab_history)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(painterResource(Res.drawable.arrow_back), null)
                        }
                    },
                    actions = {
                        var isDropdownMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { isDropdownMenuExpanded = true }) {
                            Icon(painterResource(Res.drawable.more_vert), null)
                        }
                        DropdownMenu(
                            expanded = isDropdownMenuExpanded,
                            onDismissRequest = { isDropdownMenuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.clear_all)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.close), null) },
                                onClick = {
                                    isDropdownMenuExpanded = false
                                    model.clearAll()
                                },
                                enabled = entries.value.isNotEmpty(),
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.help)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.help), null) },
                                onClick = {
                                    isDropdownMenuExpanded = false
                                    systemInteractionService.openOnlineDocumentation()
                                }
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            when {
                entries.value.isEmpty() -> {
                    Box(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(painterResource(Res.drawable.music_note), null, Modifier.size(64.dp))
                            Text(stringResource(Res.string.history_empty))
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(entries.value) { entry ->
                            HistoryEntryRow(
                                entry = entry,
                                onPlay = { replay(entry) },
                                onRemove = { model.removeEntry(entry) },
                            )
                        }
                    }
                }
            }
        }

        val unresolvedEntry = missingEntry
        if (unresolvedEntry != null) {
            val textButtonColors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
            AlertDialog(
                onDismissRequest = { missingEntry = null },
                title = { Text(stringResource(Res.string.history_file_missing_title)) },
                text = { Text(stringResource(Res.string.history_file_missing_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            model.removeEntry(unresolvedEntry)
                            missingEntry = null
                        },
                        colors = textButtonColors,
                    ) {
                        Text(stringResource(Res.string.history_clear_invalid_song))
                    }
                },
            )
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: PlaybackHistoryEntry,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val timestamp = remember(entry.playedAtEpochMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.playedAtEpochMillis))
    }
    val songTitle = if (entry.title.isBlank()) stringResource(Res.string.history_unknown_song) else entry.title
    val parentFolderPath = remember(entry.filePath) {
        File(entry.filePath).parent ?: entry.filePath
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPlay) {
            Icon(painterResource(Res.drawable.play_arrow), stringResource(Res.string.play))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(songTitle, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(timestamp, style = MaterialTheme.typography.labelSmall)
            Text(parentFolderPath, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(painterResource(Res.drawable.close), null)
        }
    }
}
