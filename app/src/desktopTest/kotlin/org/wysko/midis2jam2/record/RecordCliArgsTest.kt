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

import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reading `midis2jam2 song.mid --record out.mp4 [options]`.
 *
 * A mistake on the command line should say what's wrong before anything starts, rather than failing halfway through
 * a render, and a plain `midis2jam2 song.mid` must keep playing the song as it always has.
 */
class RecordCliArgsTest {
    private val directory = createTempDirectory("record-cli-test").toFile()
    private val midi = File(directory, "song.mid").apply { writeText("not really MIDI") }

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `a file on its own is played, not recorded`() {
        assertNull(RecordCliArgs.parse(arrayOf(midi.path)))
    }

    @Test
    @Spec("record.cli.record")
    fun `a record command names the file and where to save the video`() {
        val parsed = assertIs<RecordCliArgs.Record>(parse(midi.path, "--record", "out.mp4"))

        assertEquals(midi.path, parsed.midiFile.path)
        assertEquals("out.mp4", parsed.output.path)
    }

    @Test
    fun `options can come before or after the file`() {
        val parsed = assertIs<RecordCliArgs.Record>(
            parse("--fps", "30", "--record", "out.mp4", midi.path, "--quality", "LOW", "--resolution", "720p")
        )

        assertEquals(30, parsed.fps)
        assertEquals(VideoQuality.Low, parsed.quality)
        assertEquals(1280 to 720, parsed.resolution)
    }

    @Test
    fun `a resolution can be given as width by height`() {
        val parsed = assertIs<RecordCliArgs.Record>(parse(midi.path, "--record", "o.mp4", "--resolution", "1000x500"))

        assertEquals(1000 to 500, parsed.resolution)
    }

    @Test
    @Spec("record.cli.defaults")
    fun `options left out come from the record tab's last choices`() {
        val parsed = assertIs<RecordCliArgs.Record>(parse(midi.path, "--record", "o.mp4", "--fps", "24"))
        val defaults = RecordTabState(VideoResolution.Qhd, fps = 50, quality = VideoQuality.Maximum, soundbank = null)

        val options = parsed.toOptions(defaults)

        assertEquals(24, options.fps, "Given on the command line")
        assertEquals(2560, options.width, "From the record tab")
        assertEquals(1440, options.height, "From the record tab")
        assertEquals(VideoQuality.Maximum, options.quality, "From the record tab")
    }

    @Test
    @Spec("record.cli.invalid")
    fun `mistakes are explained rather than recorded`() {
        val mistakes = mapOf(
            "no output" to arrayOf(midi.path, "--record"),
            "no file" to arrayOf("--record", "out.mp4"),
            "missing file" to arrayOf(File(directory, "nope.mid").path, "--record", "out.mp4"),
            "two files" to arrayOf(midi.path, midi.path, "--record", "out.mp4"),
            "odd frame rate" to arrayOf(midi.path, "--record", "o.mp4", "--fps", "29"),
            "odd resolution" to arrayOf(midi.path, "--record", "o.mp4", "--resolution", "1001x500"),
            "unknown quality" to arrayOf(midi.path, "--record", "o.mp4", "--quality", "ultra"),
            "unknown option" to arrayOf(midi.path, "--record", "o.mp4", "--loop"),
            "missing soundbank" to arrayOf(midi.path, "--record", "o.mp4", "--soundbank", "nope.sf2"),
            "repeated option" to arrayOf(midi.path, "--record", "a.mp4", "--record", "b.mp4"),
        )
        mistakes.forEach { (name, args) ->
            val parsed = RecordCliArgs.parse(args)
            assertIs<RecordCliArgs.Invalid>(parsed, "$name should be rejected, but was $parsed")
            assertTrue(parsed.message.isNotBlank(), "$name should say what's wrong")
        }
    }

    private fun parse(vararg args: String) = RecordCliArgs.parse(arrayOf(*args))
}
