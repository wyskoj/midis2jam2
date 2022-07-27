package org.wysko.midis2jam2.midi.search

import org.wysko.midis2jam2.midi.StandardMidiFileReader
import java.io.File
import java.io.FileFilter
import javax.sound.midi.ShortMessage
import javax.swing.SwingUtilities

private val MIDI_FILE_FILTER = FileFilter() {
    it.extension == "mid" || it.extension == "midi"
}

object Searcher {
    fun collectMidiFilePatches(
        dir: File,
        recursive: Boolean = false,
        progress: (Int) -> Unit,
        onFinish: () -> Unit
    ): Map<File, Set<Int>> {
        val listOfFiles = if (recursive) {
            dir.walkTopDown().toList()
        } else {
            dir.listFiles().toList()
        }.filter { it.canRead() && MIDI_FILE_FILTER.accept(it) }

        return listOfFiles
            .mapIndexedNotNull { index, file ->
                try {
                    file to StandardMidiFileReader().getSequence(file)
                } catch (e: Throwable) {
                    null
                } finally {
                    SwingUtilities.invokeLater { progress((index * 100) / listOfFiles.size) }
                }
            }.also {
                SwingUtilities.invokeLater { progress(100) }
            }.associate {
                it.first.relativeTo(dir) to it.second.tracks.fold(
                    mutableSetOf<Int>()
                ) { acc, track ->
                    for (i in 0 until track.size()) {
                        with(track[i].message) {
                            if (this is ShortMessage && this.command == ShortMessage.PROGRAM_CHANGE) {
                                acc.add(this.data1)
                            }
                        }
                    }
                    acc
                }
            }.also {
                onFinish()
            }
    }
}