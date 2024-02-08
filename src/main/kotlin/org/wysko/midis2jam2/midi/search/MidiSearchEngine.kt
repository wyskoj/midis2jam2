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

package org.wysko.midis2jam2.midi.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wysko.midis2jam2.midi.StandardMidiFileReader
import java.io.File
import javax.sound.midi.MidiMessage
import javax.sound.midi.Sequence
import javax.sound.midi.ShortMessage
import javax.sound.midi.Track

/**
 * Represents the list of file extensions for MIDI files.
 */
val MIDI_FILE_EXTENSIONS: List<String> = listOf("mid", "midi", "kar")

/**
 * A class to perform MIDI file searching and indexing operations.
 * It can search for MIDI files in a directory and build an index of the instruments used in the files.
 * The index can be used to find MIDI files that use a specific instrument.
 *
 * @param directory The directory to search for MIDI files.
 * @param searchRecursively Whether to search the directory recursively.
 * @constructor Creates a new instance of the class.
 */
class MidiSearchEngine(
    private val directory: File,
    private val searchRecursively: Boolean,
) {

    /**
     * Maps MIDI files to a list of the instruments used in the file.
     * **This property is only valid after `buildIndex`
     * has finished successfully.**
     */
    lateinit var index: Map<File, List<Int>>

    private var indexJob: Job? = null
    private val midiFileReader = StandardMidiFileReader()

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
         * @param value The percentage of the total progress, from 0 to 100.
         */
        data class Percentage(val value: Int) : IndexingProgress()
    }

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
        onError: (Throwable) -> Unit = {}
    ) {
        indexJob = CoroutineScope(IO).launch {
            try {
                onProgress(IndexingProgress.Indeterminate)
                val midiFiles = if (searchRecursively) {
                    searchRecursively(directory)
                } else {
                    searchNonRecursively(directory)
                }
                val asSequences = extractSequencesFromMidiFiles(midiFiles, onProgress)
                index = buildProgramChangeIndex(asSequences)

                onFinish()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    /** Cancels the currently running index job, if any. */
    fun cancelIndex() {
        indexJob?.cancel()
    }

    /**
     * Extracts sequences from MIDI files.
     *
     * @param midiFiles The list of MIDI files to extract sequences from.
     * @param onProgress Callback function to report progress during extraction.
     * It takes an integer parameter representing the progress percentage.
     * @return A map containing the MIDI files as keys and their corresponding sequences as values.
     * Returns an empty map if no sequences were extracted.
     */
    private fun extractSequencesFromMidiFiles(
        midiFiles: List<File>,
        onProgress: (IndexingProgress) -> Unit
    ): Map<File, Sequence> = midiFiles.mapIndexedNotNull { index, file ->
        runCatching { midiFileReader.getSequence(file) }.getOrNull()?.let { file to it }
            .also {
                onProgress(IndexingProgress.Percentage((index + 1) * 100 / midiFiles.size))
            }
    }.toMap()

    /**
     * Builds the program change index for a given collection of sequences.
     *
     * @param sequences The collection of sequences to process. The keys represent the files and the values represent the sequences.
     * @return A map that associates each file with a list of program change events extracted from its corresponding sequence.
     */
    private fun buildProgramChangeIndex(sequences: Map<File, Sequence>): Map<File, List<Int>> =
        sequences.mapValues { (_, sequence) -> extractProgramChangeEvents(sequence) }

    /**
     * Extracts program change events from the given sequence.
     *
     * @param sequence The input sequence from which to extract program change events.
     * @return A list of integers representing the program change events extracted from the sequence.
     */
    private fun extractProgramChangeEvents(sequence: Sequence): List<Int> =
        sequence.tracks.flatMap { track -> extractDataFromEvents(track) }

    /**
     * Extracts data from the given track's events by processing the message data.
     *
     * @param track The track from which to extract the data.
     * @return The list of extracted data integers.
     */
    private fun extractDataFromEvents(track: Track): List<Int> =
        track.events().mapNotNull { event -> extractProgramChangeData1Value(event.message) }

    /**
     * Processes the provided MIDI message and returns the data1 value if the message is a program change
     * command (ShortMessage.PROGRAM_CHANGE).
     *
     * @param message The MIDI message to process.
     * @return The data1 value of the message if it's a program change command, otherwise null.
     */
    private fun extractProgramChangeData1Value(message: MidiMessage): Int? =
        (message as? ShortMessage)?.takeIf { it.command == ShortMessage.PROGRAM_CHANGE }?.data1

    /**
     * Search recursively for MIDI files in the given directory.
     *
     * @param directory The directory to search in.
     * @return A list of MIDI files found in the directory and its subdirectories.
     */
    private fun searchRecursively(directory: File): List<File> = directory.walk().filter { isMidiFile(it) }.toList()

    /**
     * Searches for MIDI files in the specified directory non-recursively.
     *
     * @param directory The directory to search in.
     * @return A list of MIDI files found in the directory.
     */
    private fun searchNonRecursively(directory: File): List<File> = directory.listFiles()?.filter { file ->
        isMidiFile(file)
    } ?: emptyList()

    /**
     * Checks whether the given file is a MIDI file.
     * A file is considered a MIDI file if its extension is one of the following: `mid`, `midi`, or `kar`.
     *
     * @param file The file to check.
     * @return `true` if the file is a MIDI file, `false` otherwise.
     */
    private fun isMidiFile(file: File) = file.isFile && file.extension.lowercase() in MIDI_FILE_EXTENSIONS
}

/**
 * Returns a list of events associated with the track.
 *
 * This method iterates over all the elements in the track and creates a new list containing all the events.
 *
 * @return A list of events.
 */
private fun Track.events() = (0..<size()).map { index -> get(index) }
