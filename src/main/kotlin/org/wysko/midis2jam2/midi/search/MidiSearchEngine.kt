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

package org.wysko.midis2jam2.midi.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.StandardMidiFileReader
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.readFile
import java.io.File
import java.io.IOException

/**
 * Represents the list of file extensions for MIDI files.
 */
val MIDI_FILE_EXTENSIONS: List<String> = listOf("mid", "midi", "kar")

/**
 * A class to perform MIDI file searching and indexing operations.
 * It can search for MIDI files in a directory and build an index of the instruments used in the files.
 * The index can be used to find MIDI files that use a specific instrument.
 *
 * @property directory The directory to search for MIDI files.
 * @constructor Creates a new instance of the class.
 */
class MidiSearchEngine(private val directory: File) {
    /**
     * Maps MIDI files to a list of the instruments used in the file.
     * **This property is only valid after `buildIndex`
     * has finished successfully.**
     */
    lateinit var index: Map<File, List<Byte>>

    private var indexJob: Job? = null
    private val midiFileReader = StandardMidiFileReader()

    /**
     * Builds an index of MIDI files found in a directory.
     *
     * This function is asynchronous.
     * The progress of the indexing operation can be monitored by passing
     * callbacks to the `onFinish`, `onProgress`, and `onError` parameters.
     *
     *
     * @param onFinish Function to be called when the index is finished building
     * @param onProgress Function to be called when the index is being built.
     * If the value is `Indeterminate`, the progress is indeterminate.
     * Otherwise, the value is the percentage of the index that has been built.
     * @param onError Function to be called when an error occurs during indexing.
     * @see IndexingProgress
     */
    fun buildIndex(
        onFinish: () -> Unit = {},
        onProgress: (IndexingProgress) -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        indexJob = CoroutineScope(IO).launch {
            try {
                onProgress(IndexingProgress.Indeterminate)
                val midiFiles = findMidiFilesInDirectory(directory)
                val asSequences = extractSequencesFromMidiFiles(midiFiles, onProgress)
                index = buildProgramChangeIndex(asSequences)
                onFinish()
            } catch (e: IOException) {
                onError(e)
            }
        }
    }

    /**
     * Cancels the currently running index job, if any.
     */
    fun cancelIndex() {
        indexJob?.cancel()
    }

    /**
     * Represents the progress state of the MIDI file indexing operation.
     *
     * @see buildIndex
     */
    sealed class IndexingProgress {

        /**
         * Indicates that the progress is currently indeterminate.
         */
        data object Indeterminate : IndexingProgress()

        /**
         * Indicates that the progress is a percentage of the total progress.
         *
         * @property value The percentage of the total progress, from 0 to 100.
         */
        data class Percentage(val value: Int) : IndexingProgress()
    }

    private fun extractSequencesFromMidiFiles(
        midiFiles: List<File>,
        onProgress: (IndexingProgress) -> Unit,
    ): Map<File, StandardMidiFile> =
        midiFiles.mapIndexedNotNull { index, file ->
            runCatching { midiFileReader.readFile(file) }.getOrNull()?.let { file to it }
                .also { onProgress(IndexingProgress.Percentage((index + 1) * 100 / midiFiles.size)) }
        }.toMap()

    private fun buildProgramChangeIndex(sequences: Map<File, StandardMidiFile>): Map<File, List<Byte>> =
        sequences.mapValues { (_, sequence) -> getUsedProgramValues(sequence) }

    private fun getUsedProgramValues(sequence: StandardMidiFile): List<Byte> =
        sequence.tracks.flatMap { (events) -> events.mapNotNull { event -> extractProgramChangeData1Value(event) } }

    private fun extractProgramChangeData1Value(event: Event): Byte? =
        if (event is ProgramEvent) event.program else null

    private fun findMidiFilesInDirectory(directory: File): List<File> =
        directory.walk().filter { isMidiFile(it) }.toList()

    private fun isMidiFile(file: File) = file.isFile && file.extension.lowercase() in MIDI_FILE_EXTENSIONS
}
