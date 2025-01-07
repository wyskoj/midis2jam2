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

package org.wysko.midis2jam2.gui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.midi.search.MidiSearchEngine
import org.wysko.midis2jam2.util.logger
import java.io.File

/**
 * ViewModel for the MIDI search screen.
 *
 * @property selectedInstruments The list of selected instruments.
 * @property selectedDirectory The selected directory.
 * @property matchingMidiFiles The list of MIDI files that match the selected instruments.
 * @property indexProgress The progress of the search engine's indexing process.
 * @property showProgress Whether to show the progress bar.
 * @property showError Whether to show the error dialog.
 * @constructor Creates a new instance of SearchViewModel.
 */
class SearchViewModel {
    private val _selectedInstruments = MutableStateFlow<List<String>>(emptyList())
    val selectedInstruments: StateFlow<List<String>>
        get() = _selectedInstruments

    private val _selectedDirectory = MutableStateFlow("")
    val selectedDirectory: StateFlow<String>
        get() = _selectedDirectory

    private val _matchingMidiFiles = MutableStateFlow<List<File>>(emptyList())
    val matchingMidiFiles: StateFlow<List<File>>
        get() = _matchingMidiFiles

    private val _indexProgress = MutableStateFlow(0f)
    val indexProgress: StateFlow<Float>
        get() = _indexProgress

    private val _showProgress = MutableStateFlow(false)
    val showProgress: StateFlow<Boolean>
        get() = _showProgress

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?>
        get() = _showError

    /**
     * Lazily initializes and returns a list of general MIDI instruments read from a text file.
     *
     * The general MIDI instruments are loaded from a text file named "instruments.txt."
     * If the file is not found or an error occurs during reading, an empty list is returned and an error is logged.
     *
     * @return A lazily initialized list of general MIDI instruments, or an empty list if an error occurs.
     */
    val generalMidiInstruments: List<String> by lazy {
        try {
            SearchViewModel::class.java.getResource(INSTRUMENTS_RESOURCE_PATH)?.readText()?.lines()
        } catch (exception: Exception) {
            logger().error("An error occurred while reading instruments from $INSTRUMENTS_RESOURCE_PATH.", exception)
            null
        } ?: run {
            with("Could not load general MIDI instruments.") {
                _showError.value = this
                logger().error(this)
            }
            emptyList()
        }
    }

    private lateinit var searchEngine: MidiSearchEngine

    /**
     * Selects a directory to search for MIDI files.
     *
     * @param path The path to the directory.
     */
    fun directorySelected(path: String) {
        _selectedDirectory.value = path
        buildSearchIndex()
    }

    /**
     * Adds an instrument to the selected instruments list.
     *
     * @param instrument the instrument to be added
     */
    fun addInstrument(instrument: String) {
        _selectedInstruments.value += instrument
        queryMatchingFiles()
    }

    /**
     * Removes the specified instrument from the selected instruments list.
     *
     * @param instrument The instrument to be removed.
     */
    fun removeInstrument(instrument: String) {
        _selectedInstruments.value -= instrument
        queryMatchingFiles()
    }

    /**
     * Cancels the indexing process of the search engine.
     *
     * This method checks if the search engine is initialized and cancels the indexing process if it is. The indexing process
     * is responsible for indexing data that can be searched later. If the search engine is not initialized, this method does nothing.
     *
     * @see searchEngine
     */
    fun cancelIndex() {
        if (::searchEngine.isInitialized) {
            searchEngine.cancelIndex()
        }

        // Reset progress
        _indexProgress.value = 0f
        _showProgress.value = false
        _selectedDirectory.value = ""
        _matchingMidiFiles.value = emptyList()
    }

    /**
     * Builds the search index for MIDI files in the selected directory.
     */
    private fun buildSearchIndex() {
        searchEngine = MidiSearchEngine(File(selectedDirectory.value)).also { engine ->
            val waitThenShowProgressTask = CoroutineScope(IO).launch {
                delay(1000)
                _showProgress.value = true
            }
            engine.buildIndex(onFinish = {
                queryMatchingFiles()
                waitThenShowProgressTask.cancel()
                _showProgress.value = false
            }, onProgress = {
                when (it) {
                    is MidiSearchEngine.IndexingProgress.Indeterminate -> _indexProgress.value = -1f
                    is MidiSearchEngine.IndexingProgress.Percentage -> _indexProgress.value = it.value / 100f
                }
            }, onError = {
                if (it is OutOfMemoryError) {
                    _showError.value =
                        "The selected directory contained too many files to search. Please select a smaller directory."
                    logger().warn("MIDI search indexing overflowed memory.", it)
                } else {
                    with("An error occurred while searching for MIDI files.") {
                        _showError.value = this
                        logger().error(this, it)
                    }
                }
                cancelIndex()
            })
        }
    }

    /**
     * Queries the matching files based on selected instruments.
     * If the search engine is not initialized, the method returns without performing any operations.
     *
     * The matching files are determined by filtering the search engine's index with the selected instruments.
     * Only files that contain all the selected instruments will be considered as matching.
     *
     * @return List of matching files sorted by name.
     */
    private fun queryMatchingFiles() {
        if (::searchEngine.isInitialized.not()) {
            return
        }

        _matchingMidiFiles.value = searchEngine.index.filter { (_, instruments) ->
            instruments.containsAll(selectedInstruments.value.map { generalMidiInstruments.indexOf(it).toByte() })
        }.keys.toList().sortedBy { it.name.lowercase() }
    }

    companion object {
        /**
         * Resource path to the general MIDI instruments file.
         */
        private const val INSTRUMENTS_RESOURCE_PATH = "/instruments.txt"
    }
}