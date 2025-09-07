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

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.ScreenModel
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import org.wysko.midis2jam2.midi.GENERAL_MIDI_1_PROGRAMS
import org.wysko.midis2jam2.midi.GENERAL_MIDI_1_PROGRAM_CATEGORIES
import org.wysko.midis2jam2.midi.search.MidiSearchEngine
import java.io.File

class SearchTabModel : ScreenModel {
    private val _selections = MutableStateFlow(List(128) { false })
    val selections: StateFlow<List<Boolean>>
        get() = _selections

    private val _selectedDirectory = MutableStateFlow<File?>(null)
    val selectedDirectory: StateFlow<File?>
        get() = _selectedDirectory

    private val _indexingProgress =
        MutableStateFlow<MidiSearchEngine.IndexingProgress?>(null)
    val indexingProgress: StateFlow<MidiSearchEngine.IndexingProgress?>
        get() = _indexingProgress

    private val _isShowCriteria = MutableStateFlow(true)
    val isShowCriteria: StateFlow<Boolean>
        get() = _isShowCriteria

    private var searchEngine: MutableStateFlow<MidiSearchEngine?> = MutableStateFlow(null)

    private var _isIndexAvailable = MutableStateFlow(false)
    val isIndexAvailable: StateFlow<Boolean>
        get() = _isIndexAvailable

    private var cancelling = false

    val matchingMidiFiles: Flow<List<File>> = combine(
        _selections,
        _selectedDirectory,
        _isIndexAvailable,
    ) { selections, _, _ ->
        if (searchEngine.value == null || !(searchEngine.value!!.isIndexBuilt())) {
            return@combine listOf()
        }

        if (selections.all { !it }) {
            return@combine searchEngine.value!!.index.keys.toList().sortedBy { it.name.lowercase() }
        }

        searchEngine.value!!.index.filter { (_, programs) ->
            selections.withIndex().filter { it.value }.all { (index, isSelected) ->
                isSelected && programs.contains(index.toByte())
            }
        }.keys.toList().sorted().sortedBy { it.name.lowercase() }
    }

    fun setSelection(index: Int, isSelected: Boolean) {
        _selections.value = _selections.value.toMutableList().also {
            it[index] = isSelected
        }
    }

    fun deselectAll() {
        _selections.value = List(128) { false }
    }

    fun getCategories(): List<GeneralMidiCategory> = GENERAL_MIDI_1_PROGRAMS.chunked(8).mapIndexed { index, programs ->
        GENERAL_MIDI_1_PROGRAM_CATEGORIES[index] to programs
    }

    fun setShowCriteria(isShow: Boolean) {
        _isShowCriteria.value = isShow
    }

    fun cancelIndexing() {
        cancelling = true
        searchEngine.value?.cancelIndex()
        _indexingProgress.value = null
        _selectedDirectory.value = null
        _isShowCriteria.value = true
        _isIndexAvailable.value = false
    }

    @Composable
    fun directoryPicker(
        startShowWaitingPageJob: () -> Unit = {},
        onFinish: () -> Unit = {},
    ): PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Select directory",
    ) { platformDirectory ->
        platformDirectory?.let {
            startShowWaitingPageJob()
            _isIndexAvailable.value = false
            indexDirectory(
                directory = it.file,
                onFinish = {
                    onFinish()
                }
            )
        }
    }

    private fun indexDirectory(
        directory: File,
        onFinish: () -> Unit = {},
    ) {
        cancelling = false
        _selectedDirectory.value = directory
        searchEngine.value = MidiSearchEngine(directory).also { engine ->
            engine.buildIndex(
                onFinish = {
                    _indexingProgress.value = null
                    _isIndexAvailable.value = true
                    onFinish()
                },
                onProgress = {
                    if (!cancelling) {
                        _indexingProgress.value = it
                    }
                },
                onError = {
                    _indexingProgress.value = null
                    _isIndexAvailable.value = false
                }
            )
        }
    }
}

typealias GeneralMidiCategory = Pair<String, List<String>>
