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

package org.wysko.midis2jam2.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.close
import midis2jam2.app.generated.resources.deselect
import midis2jam2.app.generated.resources.help
import midis2jam2.app.generated.resources.more_vert
import midis2jam2.app.generated.resources.search
import midis2jam2.app.generated.resources.search_criteria_too_narrow
import midis2jam2.app.generated.resources.search_deselect_all
import midis2jam2.app.generated.resources.search_directory
import midis2jam2.app.generated.resources.search_index_not_available
import midis2jam2.app.generated.resources.search_index_waiting
import midis2jam2.app.generated.resources.tab_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.midi.search.MidiSearchEngine
import org.wysko.midis2jam2.ui.common.component.CategoryHeader
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import org.wysko.midis2jam2.ui.home.HomeTab
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

object SearchTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(0u, "Search")

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinScreenModel<SearchTabModel>()
        val scope = rememberCoroutineScope()

        val indexingProgress = model.indexingProgress.collectAsState()
        val selections = model.selections.collectAsState()
        val selectedDirectory = model.selectedDirectory.collectAsState()
        val isNotShowWaitingMessage = model.isShowCriteria.collectAsState()
        val matchingMidiFiles = model.matchingMidiFiles.collectAsState(initial = listOf())
        val isIndexAvailable by model.isIndexAvailable.collectAsState()
        var displayWaitingPageJob: Job? = null

        val categories = model.getCategories()
        val directoryPicker = model.directoryPicker(startShowWaitingPageJob = {
            displayWaitingPageJob = scope.launch {
                delay(500L)
                model.setShowCriteria(false)
            }
        }, onFinish = {
            displayWaitingPageJob?.cancel()
            model.setShowCriteria(true)
        })

        Scaffold(
            topBar = {
                SearchTabTopAppBar(model, isNotShowWaitingMessage)
            },
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                IndexingProgressIndicator(indexingProgress)
                DirectoryBox(selectedDirectory, indexingProgress, directoryPicker)
                when (!isNotShowWaitingMessage.value) {
                    true -> IndexWaitingMessage()
                    false -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(Modifier.weight(1f)) {
                                InstrumentList(model, categories, selections)
                            }
                            Box(Modifier.weight(1f)) {
                                when {
                                    indexingProgress.value != null -> Unit
                                    !isIndexAvailable -> IndexNotAvailableMessage()
                                    matchingMidiFiles.value.isEmpty() -> SearchCriteriaTooNarrowMessage()
                                    else -> MidiFileList(matchingMidiFiles, selectedDirectory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("NewApi")
    @Composable
    private fun MidiFileList(
        matchingMidiFiles: State<List<File>>,
        selectedDirectory: State<File?>,
    ) {
        val midiFileListState = rememberLazyListState()
        val navigationModel = koinInject<NavigationModel>()
        val tabNavigator = LocalTabNavigator.current

        Box {
            LazyColumn(state = midiFileListState) {
                items(matchingMidiFiles.value) {
                    Row(
                        modifier = Modifier.clickable {
                            navigationModel.setApplyHomeScreenMidiFile(it)
                            tabNavigator.current = HomeTab
                        }.fillMaxWidth()
                    ) {
                        val basePath = Paths.get((selectedDirectory.value ?: return@Row).absolutePath)
                        val fileAbsolutePath = Paths.get(it.absolutePath)
                        val relativePath = basePath.relativize(fileAbsolutePath.parent).toString()
                        Column(
                            Modifier.padding(8.dp),
                        ) {
                            if (relativePath.isNotEmpty()) {
                                Text(
                                    text = "$relativePath${FileSystems.getDefault().separator}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                text = it.name,
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                adapter = rememberScrollbarAdapter(
                    scrollState = midiFileListState
                ),
                style = LocalScrollbarStyle.current
            )
        }
    }

    @Composable
    private fun IndexNotAvailableMessage() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LargeSearchIcon()
            Text(
                text = stringResource(Res.string.search_index_not_available),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun SearchCriteriaTooNarrowMessage() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LargeSearchIcon()
            Text(
                text = stringResource(Res.string.search_criteria_too_narrow),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun IndexWaitingMessage() {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LargeSearchIcon()
                Text(
                    stringResource(Res.string.search_index_waiting),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    @Composable
    private fun InstrumentList(
        model: SearchTabModel,
        categories: List<GeneralMidiCategory>,
        selections: State<List<Boolean>>,
    ) {
        val instrumentListState = rememberLazyListState()
        Box {
            LazyColumn(
                state = instrumentListState,
            ) {
                items(categories.size) { i ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryHeader(categories[i].first)
                        HorizontalDivider()
                    }
                    Column {
                        categories[i].second.forEachIndexed { j, program ->
                            val programNumber = i * 8 + j
                            val interactionSource = remember { MutableInteractionSource() }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                ) {
                                    model.setSelection(programNumber, !selections.value[programNumber])
                                }.padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    text = program,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = selections.value[programNumber],
                                    onCheckedChange = {
                                        model.setSelection(programNumber, it)
                                    },
                                    interactionSource = interactionSource,
                                )
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(instrumentListState),
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
                style = LocalScrollbarStyle.current,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DirectoryBox(
        selectedDirectory: State<File?>,
        indexingProgress: State<MidiSearchEngine.IndexingProgress?>,
        directoryPicker: PickerResultLauncher,
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier.clickable(
                    onClick = {
                        directoryPicker.launch()
                    },
                    enabled = indexingProgress.value == null,
                )
            ) {
                TextFieldDefaults.DecorationBox(
                    selectedDirectory.value?.absolutePath ?: "",
                    innerTextField = {
                        Text(selectedDirectory.value?.absolutePath ?: "", Modifier.fillMaxWidth())
                    },
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    label = { Text(stringResource(Res.string.search_directory)) },
                )
            }
        }
    }

    @Composable
    private fun IndexingProgressIndicator(indexingProgress: State<MidiSearchEngine.IndexingProgress?>) {
        when (indexingProgress.value) {
            is MidiSearchEngine.IndexingProgress.Indeterminate -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            is MidiSearchEngine.IndexingProgress.Percentage -> {
                LinearProgressIndicator(
                    progress = {
                        (indexingProgress.value as? MidiSearchEngine.IndexingProgress.Percentage)?.value?.div(100f)
                            ?: 1f
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            else -> Spacer(Modifier.height(4.dp))
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun SearchTabTopAppBar(
        model: SearchTabModel,
        isShowCriteriaContent: State<Boolean>,
    ) {
        val systemInteractionService = koinInject<SystemInteractionService>()

        TopAppBar(title = {
            Column {
                Text(stringResource(Res.string.tab_search))
            }
        }, actions = {
            AnimatedVisibility(!isShowCriteriaContent.value) {
                Button(
                    onClick = model::cancelIndexing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                ) {
                    Icon(painterResource(Res.drawable.close), null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.cancel))
                }
            }
            var isMenuOpen by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    isMenuOpen = true
                }
            ) {
                Icon(painterResource(Res.drawable.more_vert), null)
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
            ) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.search_deselect_all)) }, leadingIcon = {
                    Icon(painterResource(Res.drawable.deselect), null)
                }, onClick = {
                    isMenuOpen = false
                    model.deselectAll()
                })
                HorizontalDivider()
                DropdownMenuItem(text = { Text(stringResource(Res.string.help)) }, leadingIcon = {
                    Icon(painterResource(Res.drawable.help), null)
                }, onClick = {
                    systemInteractionService.openOnlineDocumentation()
                })
            }
        })
    }

    @Composable
    private fun LargeSearchIcon() {
        Icon(painterResource(Res.drawable.search), null, Modifier.size(64.dp))
    }
}
