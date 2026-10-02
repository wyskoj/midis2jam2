/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.ui

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.StandardMidiFileWriter
import org.wysko.kmidi.midi.builder.smf
import org.wysko.midis2jam2.midi.GENERAL_MIDI_1_PROGRAMS
import org.wysko.midis2jam2.midi.GENERAL_MIDI_1_PROGRAM_CATEGORIES
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.search.SearchTabModel
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The search screen, which finds MIDI files on disk by the instruments they use.
 *
 * Indexing walks the filesystem and reads every MIDI file it finds, so these tests build a
 * small tree of real files with known program changes and search it.
 */
class SearchTabModelTest {

    @Test
    @Spec("search.scans-subfolders")
    fun `the scan reaches files in subfolders`() {
        val root = tempDirectory()
        writeMidi(File(root, "top.mid"), program = PIANO)
        val nested = File(root, "deeper/and/deeper").apply { mkdirs() }
        writeMidi(File(nested, "buried.mid"), program = PIANO)

        val model = indexed(root)

        assertEquals(
            listOf("buried.mid", "top.mid"),
            model.allFound(),
            "The scan should reach MIDI files however deeply they are nested"
        )
    }

    @Test
    fun `files that are not MIDI files are ignored`() {
        val root = tempDirectory()
        writeMidi(File(root, "song.mid"), program = PIANO)
        File(root, "notes.txt").writeText("not a midi file")
        File(root, "cover.png").writeText("not a midi file either")

        assertEquals(listOf("song.mid"), indexed(root).allFound())
    }

    @Test
    fun `a file that cannot be read is skipped rather than failing the scan`() {
        val root = tempDirectory()
        writeMidi(File(root, "good.mid"), program = PIANO)
        File(root, "broken.mid").writeText("this is not a MIDI file at all")

        assertEquals(
            listOf("good.mid"),
            indexed(root).allFound(),
            "One unreadable file should not stop the rest of the scan"
        )
    }

    @Test
    @Spec("search.filter-by-instrument")
    fun `selecting an instrument narrows the results to files that use it`() = runTest {
        val root = tempDirectory()
        writeMidi(File(root, "piano.mid"), program = PIANO)
        writeMidi(File(root, "violin.mid"), program = VIOLIN)
        writeMidi(File(root, "both.mid"), programs = listOf(PIANO, VIOLIN))

        val model = indexed(root)

        model.setSelection(PIANO, true)
        assertEquals(
            listOf("both.mid", "piano.mid"),
            model.matchingMidiFiles.first().map { it.name }.sorted(),
            "Selecting the piano should list every file that uses it"
        )
    }

    @Test
    fun `selecting several instruments lists only files that use all of them`() = runTest {
        val root = tempDirectory()
        writeMidi(File(root, "piano.mid"), program = PIANO)
        writeMidi(File(root, "violin.mid"), program = VIOLIN)
        writeMidi(File(root, "both.mid"), programs = listOf(PIANO, VIOLIN))

        val model = indexed(root)
        model.setSelection(PIANO, true)
        model.setSelection(VIOLIN, true)

        assertEquals(
            listOf("both.mid"),
            model.matchingMidiFiles.first().map { it.name },
            "Two selected instruments should list only files that use both"
        )
    }

    @Test
    fun `selecting nothing lists everything that was found`() = runTest {
        val root = tempDirectory()
        writeMidi(File(root, "piano.mid"), program = PIANO)
        writeMidi(File(root, "violin.mid"), program = VIOLIN)

        val model = indexed(root)

        assertEquals(
            listOf("piano.mid", "violin.mid"),
            model.matchingMidiFiles.first().map { it.name }.sorted()
        )
    }

    @Test
    fun `an instrument nothing uses lists nothing`() = runTest {
        val root = tempDirectory()
        writeMidi(File(root, "piano.mid"), program = PIANO)

        val model = indexed(root)
        model.setSelection(VIOLIN, true)

        assertTrue(model.matchingMidiFiles.first().isEmpty())
    }

    @Test
    @Spec("search.deselect-all")
    fun `deselect all clears the instrument selection`() = runTest {
        val root = tempDirectory()
        writeMidi(File(root, "piano.mid"), program = PIANO)
        writeMidi(File(root, "violin.mid"), program = VIOLIN)

        val model = indexed(root)
        model.setSelection(PIANO, true)
        model.setSelection(VIOLIN, true)

        model.deselectAll()

        assertTrue(model.selections.value.none { it }, "No instrument should still be selected")
        assertEquals(
            2,
            model.matchingMidiFiles.first().size,
            "With nothing selected, every found file should be listed again"
        )
    }

    @Test
    @Spec("search.cancel-scan")
    fun `cancelling a scan puts the screen back the way it was`() {
        val root = tempDirectory()
        writeMidi(File(root, "song.mid"), program = PIANO)
        val model = indexed(root)

        model.cancelIndexing()

        assertEquals(null, model.selectedDirectory.value, "Cancelling should forget the directory")
        assertEquals(null, model.indexingProgress.value, "Cancelling should clear the progress")
        assertTrue(!model.isIndexAvailable.value, "Cancelling should discard the index")
        assertTrue(model.isShowCriteria.value, "Cancelling should show the search criteria again")
    }

    @Test
    fun `the instrument list offers every General MIDI program, grouped by family`() {
        val categories = SearchTabModel().getCategories()

        assertEquals(
            GENERAL_MIDI_1_PROGRAM_CATEGORIES.size,
            categories.size,
            "Every General MIDI family should be offered"
        )
        assertEquals(
            GENERAL_MIDI_1_PROGRAMS.size,
            categories.sumOf { (_, programs) -> programs.size },
            "Every General MIDI program should appear in exactly one family"
        )
        assertEquals(
            GENERAL_MIDI_1_PROGRAM_CATEGORIES,
            categories.map { (name, _) -> name },
            "The families should be offered in the General MIDI order"
        )
    }

    @Test
    fun `no results are offered before a directory has been scanned`() = runTest {
        assertTrue(SearchTabModel().matchingMidiFiles.first().isEmpty())
    }

    private companion object {

        /** Acoustic Grand Piano and Violin, in General MIDI program numbers. */
        const val PIANO = 0
        const val VIOLIN = 40

        const val INDEX_TIMEOUT_SECONDS = 60L

        fun tempDirectory(): File =
            java.nio.file.Files.createTempDirectory("midis2jam2-search").toFile().apply { deleteOnExit() }

        /** Builds a model that has finished indexing [root]. */
        fun indexed(root: File): SearchTabModel {
            val model = SearchTabModel()
            val finished = CountDownLatch(1)
            model.indexDirectory(root) { finished.countDown() }
            if (!finished.await(INDEX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                fail("Indexing ${root.absolutePath} did not finish within ${INDEX_TIMEOUT_SECONDS}s")
            }
            return model
        }

        /** Every file the scan found, by name, in a stable order. */
        fun SearchTabModel.allFound(): List<String> = runBlocking {
            matchingMidiFiles.first().map { it.name }.sorted()
        }

        fun writeMidi(file: File, program: Int) = writeMidi(file, listOf(program))

        fun writeMidi(file: File, programs: List<Int>) {
            val midi = smf {
                format = StandardMidiFile.Header.Format.Format0
                division = tpq(96)
                track {
                    tempo(120)
                    programs.forEachIndexed { channel, program ->
                        channel(channel) {
                            program(program)
                            note(60, duration = 1.quarter)
                        }
                    }
                }
            }
            file.parentFile.mkdirs()
            file.writeBytes(StandardMidiFileWriter().write(midi))
        }
    }
}
