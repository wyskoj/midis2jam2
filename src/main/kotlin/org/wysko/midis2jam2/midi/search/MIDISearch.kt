/*
 * Copyright (C) 2023 Jacob Wysko
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

import kotlinx.coroutines.yield
import org.wysko.midis2jam2.midi.StandardMidiFileReader
import java.io.File
import java.io.FileFilter
import javax.sound.midi.ShortMessage
import javax.swing.SwingUtilities

private val MIDI_FILE_FILTER = FileFilter {
    it.extension == "mid" || it.extension == "midi"
}

/**
 * A utility object for searching and filtering MIDI files.
 */
object MIDISearch {
    /**
     * Searches for MIDI files in the given directory.
     *
     * @param dir the directory to search in
     * @param recursive whether to search recursively
     * @param progress the progress listener to use
     * @param onFinish the callback to call when the search is finished
     */
    suspend fun collectMidiFilePatches(
        dir: File,
        recursive: Boolean = false,
        progress: (Int) -> Unit,
        onFinish: () -> Unit
    ): Map<File, Set<Int>> {
        // Collect files to process
        val listOfFiles = if (recursive) {
            dir.walkTopDown().toList()
        } else {
            dir.listFiles().toList()
        }.filter { it.canRead() && MIDI_FILE_FILTER.accept(it) }

        yield()

        return listOfFiles
            // Map files to their MIDI sequence
            .mapIndexedNotNull { index, file ->
                try {
                    file to StandardMidiFileReader().getSequence(file)
                } catch (e: Throwable) {
                    null
                } finally {
                    SwingUtilities.invokeLater { progress((index * 100) / listOfFiles.size) } // Update progress
                    yield()
                }
            }.also {
                SwingUtilities.invokeLater { progress(100) } // Finished with progress
            }.associate {
                it.first.relativeTo(dir) to it.second.tracks.fold(
                    mutableSetOf<Int>()
                ) { acc, track ->
                    for (i in 0 until track.size()) { // For each event in the track
                        with(track[i].message) {
                            if (this is ShortMessage && this.command == ShortMessage.PROGRAM_CHANGE) { // If it's a program change event
                                acc.add(this.data1) // Add the program number to the set
                            }
                        }
                    }
                    acc // Return the accumulated set
                }
            }.also {
                onFinish()
            }
    }
}
