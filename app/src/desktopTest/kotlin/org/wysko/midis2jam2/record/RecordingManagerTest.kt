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

package org.wysko.midis2jam2.record

import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * What happens to the file when a recording doesn't finish.
 *
 * A half-written MP4 has no index and won't play, so leaving one behind looks like a broken recording. Whether the
 * performance is closed early or never records anything, the partial file must go.
 */
class RecordingManagerTest {
    private val directory = createTempDirectory("recording-manager-test").toFile()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    @Spec("record.cancel.removes-partial")
    fun `closing the performance before the recording finishes deletes the partial file`() {
        val listener = RecordingEvents()
        val output = File(directory, "partial.mp4").apply { writeText("half a video") }
        val manager = newManager(output, listener)

        manager.cancel()

        assertFalse(output.exists(), "The partial file should be deleted")
        assertEquals(listOf("cancelled"), listener.events)
    }

    @Test
    fun `a performance that ends before any frame is recorded reports a failure, not a video`() {
        val listener = RecordingEvents()
        val output = File(directory, "empty.mp4").apply { writeText("nothing") }
        val manager = newManager(output, listener)

        manager.complete()
        manager.cancel()

        assertFalse(output.exists(), "No file should be left behind")
        assertEquals(listOf("failed"), listener.events, "It should fail once, and not also count as cancelled")
    }

    private fun newManager(output: File, listener: RecordingListener) = RecordingManager(
        RecordOptions(outputPath = output.path, width = 64, height = 64, fps = 30),
        MidiFixtures.singleProgram(0),
        OfflineSynthesizer(soundbank = null),
        listener,
    )

    private class RecordingEvents : RecordingListener {
        val events = mutableListOf<String>()

        override fun onProgress(framesCaptured: Long, expectedFrames: Long) = Unit

        override fun onFinished(file: File) {
            events += "finished"
        }

        override fun onCancelled() {
            events += "cancelled"
        }

        override fun onFailed(exception: RecordingException) {
            events += "failed"
        }
    }
}
