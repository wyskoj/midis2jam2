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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The audio in a recording is rendered by Gervill offline, so it must line up with the song's own clock: a note
 * written at one second into the file must sound one second into the audio.
 */
class OfflineSynthesizerTest {

    @Test
    @Spec("record.audio.offline")
    fun `a note sounds at the time the file puts it, and not before`() {
        val audio = render(MidiFixtures.oneNoteAfterRest(restQuarters = 2), length = 2.seconds)

        val onset = audio.firstLoudSample()
        val expected = 1.seconds
        assertTrue(onset != null, "The note should be audible")
        assertTrue(
            abs((onset - expected).inWholeMilliseconds) <= 20,
            "The note is written at $expected into the file, but sounded at $onset"
        )
    }

    @Test
    @Spec("record.audio.sync")
    fun `leading silence delays the song by exactly that long`() {
        val synthesizer = OfflineSynthesizer(soundbank = null).apply { queue(MidiFixtures.oneNoteAfterRest(2)) }
        val audio = synthesizer.withLeadingSilence(500.milliseconds).use { it.readFor(2.seconds) }

        val onset = audio.firstLoudSample()
        assertTrue(onset != null && abs((onset - 1.5.seconds).inWholeMilliseconds) <= 20, "Onset was $onset")
    }

    private fun render(sequence: org.wysko.kmidi.midi.TimeBasedSequence, length: Duration): Samples =
        OfflineSynthesizer(soundbank = null).use { synthesizer ->
            synthesizer.queue(sequence)
            synthesizer.readFor(length)
        }

    private class Samples(val data: ShortArray, val sampleRate: Int, val channels: Int) {
        fun firstLoudSample(): Duration? {
            val index = data.indexOfFirst { abs(it.toInt()) > LOUD }
            if (index < 0) return null
            return (index / channels * 1_000_000L / sampleRate).microseconds
        }
    }

    private fun AudioSource.readFor(length: Duration): Samples {
        val total = (length.inWholeMilliseconds * sampleRate / 1000).toInt() * channels
        val data = ShortArray(total)
        var filled = 0
        val chunk = ShortArray(4096 * channels)
        while (filled < total) {
            val read = read(chunk, minOf(chunk.size, total - filled))
            if (read <= 0) break
            chunk.copyInto(data, filled, 0, read)
            filled += read
        }
        return Samples(data, sampleRate, channels)
    }

    private companion object {
        /** Well above Gervill's noise floor, well below a piano note at full velocity. */
        const val LOUD = 500
    }
}
