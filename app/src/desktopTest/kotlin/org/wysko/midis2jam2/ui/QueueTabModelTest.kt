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

import com.russhwolf.settings.PropertiesSettings
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.settings.PreferenceBackedSettingsRepository
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.queue.QueueTabModel
import org.wysko.midis2jam2.ui.settings.SettingsModel
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The queue screen: a list of MIDI files to play one after another.
 *
 * The documented operations are all list arithmetic - add, remove, reorder, shuffle, clear,
 * save and load - which is exactly the kind of code where an off-by-one hides comfortably.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueTabModelTest {

    @BeforeTest
    fun startGraph() {
        // The model resolves the settings model through Koin while it is constructing, to
        // work out whether the background is misconfigured.
        startKoin {
            modules(
                module {
                    single<SettingsRepository> {
                        PreferenceBackedSettingsRepository(PropertiesSettings(Properties()))
                    }
                    single { SettingsModel(get()) }
                }
            )
        }
    }

    @AfterTest
    fun stopGraph() {
        stopKoin()
    }

    @Test
    @Spec("queue.add.button")
    fun `files can be added to the queue`() {
        val model = newModel()

        model.addToQueue(files("a.mid", "b.mid"))

        assertEquals(listOf("a.mid", "b.mid"), model.queue.value.names())
    }

    @Test
    fun `adding files keeps the ones already queued`() {
        val model = newModel()

        model.addToQueue(files("a.mid"))
        model.addToQueue(files("b.mid"))

        assertEquals(listOf("a.mid", "b.mid"), model.queue.value.names())
    }

    @Test
    fun `the same file is not queued twice`() {
        val model = newModel()

        model.addToQueue(files("a.mid", "b.mid"))
        model.addToQueue(files("b.mid", "c.mid"))

        assertEquals(listOf("a.mid", "b.mid", "c.mid"), model.queue.value.names())
    }

    @Test
    @Spec("queue.remove")
    fun `a song can be removed from the queue`() {
        val model = newModel()
        model.addToQueue(files("a.mid", "b.mid", "c.mid"))

        model.removeAtIndex(1)

        assertEquals(listOf("a.mid", "c.mid"), model.queue.value.names())
    }

    @Test
    @Spec("queue.reorder")
    fun `a song can be dragged to a new position`() {
        val model = newModel()
        model.addToQueue(files("a.mid", "b.mid", "c.mid"))

        model.moveItem(from = 0, to = 2)

        assertEquals(listOf("b.mid", "c.mid", "a.mid"), model.queue.value.names())
    }

    @Test
    fun `dragging a song backwards works too`() {
        val model = newModel()
        model.addToQueue(files("a.mid", "b.mid", "c.mid"))

        model.moveItem(from = 2, to = 0)

        assertEquals(listOf("c.mid", "a.mid", "b.mid"), model.queue.value.names())
    }

    @Test
    fun `a drag that goes nowhere leaves the queue alone`() {
        val model = newModel()
        model.addToQueue(files("a.mid", "b.mid"))

        model.moveItem(from = 1, to = 1)
        model.moveItem(from = 5, to = 0)
        model.moveItem(from = 0, to = -1)

        assertEquals(listOf("a.mid", "b.mid"), model.queue.value.names())
    }

    @Test
    @Spec("queue.op.shuffle")
    fun `shuffling keeps every song and changes the order`() {
        val model = newModel()
        val original = (1..20).map { "song$it.mid" }
        model.addToQueue(files(*original.toTypedArray()))

        model.shuffleQueue()

        assertEquals(
            original.sorted(),
            model.queue.value.names().sorted(),
            "Shuffling must not lose or invent songs"
        )
        // With twenty songs, landing back on the original order would be a one in 20! chance.
        assertTrue(
            model.queue.value.names() != original,
            "Shuffling twenty songs returned them in the original order"
        )
    }

    @Test
    @Spec("queue.op.clear")
    fun `clearing empties the queue`() {
        val model = newModel()
        model.addToQueue(files("a.mid", "b.mid"))

        model.clearQueue()

        assertTrue(model.queue.value.isEmpty())
    }

    @Test
    @Spec("queue.op.save", "queue.op.load")
    fun `a saved queue loads back the way it was`() {
        val directory = createTempDirectory()
        val songs = (1..3).map { File(directory, "song$it.mid").apply { writeText("") } }

        val saving = newModel()
        saving.addToQueue(songs.map { PlatformFile(it) })
        val playlist = File(directory, "playlist.txt").apply { writeText(saving.asPlaylistText()) }

        val loading = newModel()
        val missing = mutableListOf<List<String>>()
        loading.applyQueue(PlatformFile(playlist)) { missing += it }

        assertEquals(
            songs.map { it.name },
            loading.queue.value.names(),
            "A queue saved to a file should come back in the same order"
        )
        assertTrue(missing.isEmpty(), "Nothing should have been reported missing: $missing")
    }

    @Test
    fun `loading a playlist reports the songs that are no longer there`() {
        val directory = createTempDirectory()
        val present = File(directory, "here.mid").apply { writeText("") }
        val absent = File(directory, "gone.mid")
        val playlist = File(directory, "playlist.txt").apply {
            writeText(present.absolutePath + "\n" + absent.absolutePath)
        }

        val model = newModel()
        val missing = mutableListOf<List<String>>()
        model.applyQueue(PlatformFile(playlist)) { missing += it }

        assertEquals(listOf("here.mid"), model.queue.value.names(), "Only the songs that exist should load")
        assertEquals(
            listOf(listOf(absent.absolutePath)),
            missing,
            "The missing song should be reported rather than silently dropped"
        )
    }

    @Test
    @Spec("queue.play-button-requires-song")
    fun `the play button is offered only when there is something to play`() = runTest {
        val model = newModel()

        assertTrue(!model.isPlayButtonEnabled.first(), "An empty queue should not offer Play")

        model.addToQueue(files("a.mid"))
        assertTrue(model.isPlayButtonEnabled.first(), "A queue with a song should offer Play")

        model.clearQueue()
        assertTrue(!model.isPlayButtonEnabled.first(), "Clearing the queue should withdraw Play")
    }

    @Test
    fun `playing an empty queue is refused rather than attempted`() {
        assertFailsWith<IllegalStateException> { newModel().startApplication() }
    }

    @Test
    fun `the queue is marked unsaved once it is changed`() {
        val model = newModel()
        assertTrue(!model.isDirty.value, "A new queue has nothing to save")

        model.addToQueue(files("a.mid"))
        assertTrue(model.isDirty.value, "Adding a song should mark the queue unsaved")

        model.setIsDirty(false)
        model.shuffleQueue()
        assertTrue(model.isDirty.value, "Shuffling should mark the queue unsaved")

        model.setIsDirty(false)
        model.moveItem(from = 0, to = 0)
        assertTrue(!model.isDirty.value, "A drag that changed nothing should not mark the queue unsaved")
    }

    private companion object {

        fun newModel() = QueueTabModel(ApplicationService())

        /** Files in one shared folder, so the same name means the same file. */
        fun files(vararg names: String): List<PlatformFile> {
            val directory = sharedDirectory
            return names.map { PlatformFile(File(directory, it)) }
        }

        val sharedDirectory: File by lazy { createTempDirectory() }

        fun List<PlatformFile>.names(): List<String> = map { it.file.name }

        fun createTempDirectory(): File =
            java.nio.file.Files.createTempDirectory("midis2jam2-queue").toFile().apply { deleteOnExit() }
    }
}
