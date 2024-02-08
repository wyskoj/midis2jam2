/*
 * Copyright (C) 2024 Jacob Wysko
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.gui.viewmodel.I18n
import org.wysko.midis2jam2.gui.viewmodel.SearchViewModel
import java.io.File

/**
 * Displays the search screen.
 *
 * @param searchViewModel The view model for the search screen.
 * @param snackbarHost The state for the snackbar host.
 * @param onMidiFileSelected A callback function that is invoked when a MIDI file is selected.
 */
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel, snackbarHost: SnackbarHostState, onMidiFileSelected: (File) -> Unit = {}
) {
    val selectedInstruments by searchViewModel.selectedInstruments.collectAsState()
    val selectedDirectory by searchViewModel.selectedDirectory.collectAsState()
    var showDirPicker by remember { mutableStateOf(false) }
    val matchingMidiFiles by searchViewModel.matchingMidiFiles.collectAsState()
    val indexProgress by searchViewModel.indexProgress.collectAsState()
    val showProgress by searchViewModel.showProgress.collectAsState()
    val showError by searchViewModel.showError.collectAsState()
    val scope = rememberCoroutineScope()
    var focusIndex by remember { mutableStateOf(0) }

    DirectoryPickerComponent(showDirPicker, { showDirPicker = it }, searchViewModel, focusIndex) {
        focusIndex = it
    }
    ProgressIndicatorComponent(showProgress, indexProgress)
    ErrorSnackbarComponent(showError, snackbarHost, scope)

    val searchScreenData = SearchScreenData(
        searchViewModel = searchViewModel,
        selectedInstruments = selectedInstruments,
        matchingMidiFiles = matchingMidiFiles
    )

    val searchScreenActions = SearchScreenActions(showDirPicker = { showDirPicker = true },
        onMidiFileSelected = onMidiFileSelected,
        cancelIndex = { searchViewModel.cancelIndex() })

    SearchScreenContents(
        searchScreenData = searchScreenData,
        searchScreenActions = searchScreenActions,
        selectedDirectory = selectedDirectory,
        showProgress = showProgress,
        focusIndex = focusIndex
    )
}

@Composable
private fun DirectoryPickerComponent(
    showDirPicker: Boolean,
    setShowDirPicker: (Boolean) -> Unit,
    searchViewModel: SearchViewModel,
    focusIndex: Int,
    setFocusIndex: (Int) -> Unit
) {
    DirectoryPicker(showDirPicker) { path ->
        setShowDirPicker(false)
        path?.let {
            searchViewModel.directorySelected(it)
        }
        setFocusIndex(focusIndex + 1)
    }
}

@Composable
private fun ProgressIndicatorComponent(
    showProgress: Boolean, indexProgress: Float
) {
    if (!showProgress) {
        return
    }
    if (indexProgress < 0f) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else if (indexProgress != 0f) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(), progress = indexProgress
        )
    } else {
        // Don't show progress indicator if progress is 0
    }
}

@Composable
private fun ErrorSnackbarComponent(
    showError: String?, snackbarHost: SnackbarHostState, scope: CoroutineScope
) {
    LaunchedEffect(showError) {
        if (showError != null) {
            scope.launch {
                snackbarHost.showSnackbar(
                    message = showError, actionLabel = I18n["dismiss"].value, duration = SnackbarDuration.Long
                )
            }
        }
    }
}

private data class SearchScreenData(
    val searchViewModel: SearchViewModel, val selectedInstruments: List<String>, val matchingMidiFiles: List<File>
)

private data class SearchScreenActions(
    val showDirPicker: () -> Unit, val onMidiFileSelected: (File) -> Unit, val cancelIndex: () -> Unit = {}
)

@Composable
private fun SearchScreenContents(
    searchScreenData: SearchScreenData,
    searchScreenActions: SearchScreenActions,
    selectedDirectory: String,
    showProgress: Boolean,
    focusIndex: Int
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        HeaderTexts(showProgress, searchScreenActions.cancelIndex)
        DirectoryTextField(searchScreenActions.showDirPicker, selectedDirectory, focusIndex)
        Spacer(modifier = Modifier.height(16.dp))
        Columns(
            searchScreenData.searchViewModel,
            searchScreenData.selectedInstruments,
            searchScreenData.matchingMidiFiles,
            searchScreenActions.onMidiFileSelected
        )
    }
}

@Composable
private fun HeaderTexts(
    showProgress: Boolean, cancelIndex: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(I18n["search_title"].value, style = MaterialTheme.typography.headlineSmall)
            Text(
                I18n["search_description"].value, style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        AnimatedVisibility(showProgress) {
            ElevatedButton(
                cancelIndex, colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = I18n["cancel"].value)
                Spacer(modifier = Modifier.width(8.dp))
                Text(I18n["cancel"].value)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectoryTextField(showDirPicker: () -> Unit, selectedDirectory: String, focusIndex: Int) {
    val focusRequester = remember { FocusRequester() }
    Row(
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextField(modifier = Modifier.focusable(false).fillMaxWidth().focusRequester(focusRequester),
            value = selectedDirectory,
            onValueChange = { },
            label = { Text(I18n["search_directory"].value) },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                IconButton(showDirPicker, Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Icon(Icons.Default.List, contentDescription = I18n["browse"].value)
                }
            })
    }

    DisposableEffect(focusIndex) {
        if (focusIndex != 0) {
            focusRequester.requestFocus()
        }
        onDispose { }
    }
}

@Composable
private fun Columns(
    searchViewModel: SearchViewModel,
    selectedInstruments: List<String>,
    matchingMidiFiles: List<File>,
    onMidiFileSelected: (File) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        InstrumentsList(
            title = I18n["search_available_instruments"].value,
            instruments = with(searchViewModel) { generalMidiInstruments.minus(selectedInstruments.toSet()) },
            addToSearchModifier = { instrument -> searchViewModel.addInstrument(instrument) },
            icon = Icons.Default.ArrowForward,
            iconDescription = I18n["search_add"].value,
            modifier = Modifier.weight(1f)
        )

        InstrumentsList(
            title = I18n["search_selected_instruments"].value,
            instruments = selectedInstruments,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            addToSearchModifier = { instrument -> searchViewModel.removeInstrument(instrument) },
            icon = Icons.Default.ArrowBack,
            iconDescription = I18n["search_remove"].value,
            modifier = Modifier.weight(1f)
        )

        MidiFileList(
            matchingMidiFiles = matchingMidiFiles,
            onMidiFileSelected = onMidiFileSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InstrumentsList(
    modifier: Modifier = Modifier,
    title: String,
    instruments: List<String>,
    colors: CardColors = CardDefaults.cardColors(),
    addToSearchModifier: (String) -> Unit,
    icon: ImageVector,
    iconDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(instruments) { instrument ->
                InstrumentCard(instrument, colors, addToSearchModifier, icon, iconDescription)
            }
        }
    }
}

@Composable
private fun InstrumentCard(
    instrument: String,
    colors: CardColors,
    addToSearchModifier: (String) -> Unit,
    icon: ImageVector,
    iconDescription: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = colors
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { addToSearchModifier(instrument) }) {
                Icon(icon, contentDescription = iconDescription)
            }
            Text(
                instrument, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp).weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MidiFileList(
    matchingMidiFiles: List<File>, onMidiFileSelected: (File) -> Unit, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            I18n["search_matching_midi_files"].value, style = MaterialTheme.typography.labelLarge
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(matchingMidiFiles) { file ->
                MidiFileCard(file, onMidiFileSelected)
            }
        }
    }
}

@Composable
private fun MidiFileCard(file: File, onMidiFileSelected: (File) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            IconButton(onClick = { onMidiFileSelected(file) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = I18n["load"].value)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}