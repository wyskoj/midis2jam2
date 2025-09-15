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

package org.wysko.midis2jam2.ui.home.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.arrow_back
import midis2jam2.app.generated.resources.clear_all
import midis2jam2.app.generated.resources.content_copy
import midis2jam2.app.generated.resources.file_open
import midis2jam2.app.generated.resources.log_clear
import midis2jam2.app.generated.resources.log_clear_all
import midis2jam2.app.generated.resources.log_cleared_all_entries
import midis2jam2.app.generated.resources.log_cleared_entry
import midis2jam2.app.generated.resources.log_copied_to_clipboard
import midis2jam2.app.generated.resources.log_copy_to_clipboard
import midis2jam2.app.generated.resources.log_empty_message
import midis2jam2.app.generated.resources.log_title
import midis2jam2.app.generated.resources.more_vert
import midis2jam2.app.generated.resources.remove
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.ErrorLogEntry
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.util.Utils
import java.text.SimpleDateFormat

object LogScreen : Screen {
    @Suppress("AssignedValueIsNeverRead")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinInject<ErrorLogService>()
        val errors = model.errors.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val logClearedAllEntriesMessage = stringResource(Res.string.log_cleared_all_entries)
        val logClearedEntryMessage = stringResource(Res.string.log_cleared_entry)
        val logCopiedToClipboardMessage = stringResource(Res.string.log_copied_to_clipboard)

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(Res.string.log_title)) }, navigationIcon = {
                    IconButton(onClick = {
                        navigator.pop()
                        model.markAllAsRead()
                    }) {
                        Icon(painterResource(Res.drawable.arrow_back), null)
                    }
                }, actions = {
                    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
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
                            text = { Text(stringResource(Res.string.log_clear_all)) },
                            leadingIcon = { Icon(painterResource(Res.drawable.clear_all), "") },
                            onClick = {
                                isDropdownMenuExpanded = false
                                model.clearAll()
                                scope.launch {
                                    snackbarHostState.showSnackbar(logClearedAllEntriesMessage)
                                }
                            },
                            enabled = errors.value.isNotEmpty(),
                        )
                    }
                })
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
        ) { paddingValues ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (errors.value.size) {
                    0 -> item {
                        Text(
                            text = stringResource(Res.string.log_empty_message),
                            fontStyle = FontStyle.Italic
                        )
                    }

                    else -> items(errors.value) { error ->
                        LogEntryCard(
                            error,
                            onClear = {
                                model.removeError(error)
                                scope.launch {
                                    snackbarHostState.showSnackbar(logClearedEntryMessage)
                                }
                            },
                            onCopyClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(logCopiedToClipboardMessage)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun LogEntryCard(
        error: ErrorLogEntry,
        onCopyClick: () -> Unit = {},
        onClear: (ErrorLogEntry) -> Unit = {},
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatDate(error.timestamp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = error.message,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box {
                        var isDropdownMenuExpanded by remember { mutableStateOf(false) }
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
                                text = { Text(stringResource(Res.string.log_copy_to_clipboard)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.content_copy), "") },
                                onClick = {
                                    isDropdownMenuExpanded = false
                                    Utils.copyToClipboard(error.throwable?.stackTraceToString() ?: "")
                                    onCopyClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.log_clear)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.remove), "") },
                                onClick = {
                                    isDropdownMenuExpanded = false
                                    onClear(error)
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
                Text(
                    text = error.throwable?.stackTraceToString() ?: "",
                    style = TextStyle(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat.getDateTimeInstance().format(java.util.Date(timestamp))
}
