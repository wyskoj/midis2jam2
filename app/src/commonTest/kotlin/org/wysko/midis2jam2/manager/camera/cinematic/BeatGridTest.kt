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

package org.wysko.midis2jam2.manager.camera.cinematic

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.builder.smf
import org.wysko.midis2jam2.manager.camera.cinematic.analysis.BeatGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The beats and bar lines the cinematic camera cuts on.
 *
 * If these drift from the music, every cut lands off the beat, which is the first thing a viewer notices about a
 * badly edited concert film.
 */
class BeatGridTest {

    @Test
    fun `a uniform grid places beats and bars at a steady pace`() {
        val grid = BeatGrid.uniform(secondsPerBeat = 0.5, beatsPerBar = 4, duration = 10.0)

        assertEquals(0.0, grid.timeOf(0), TOLERANCE)
        assertEquals(2.0, grid.timeOf(4), TOLERANCE)
        assertTrue(grid.isBarStart(4), "Beat 4 should begin the second bar of 4/4")
        assertTrue(!grid.isBarStart(5), "Beat 5 is in the middle of a bar")
        assertEquals(4, grid.beatsPerBarAt(6))
        assertEquals(2.0, grid.secondsPerBarAt(3.0), TOLERANCE)
    }

    @Test
    fun `a time falls in the beat that began before it`() {
        val grid = BeatGrid.uniform(0.5, 4, 10.0)

        assertEquals(0, grid.beatAt(-3.0), "Times before the song belong to the first beat")
        assertEquals(2, grid.beatAt(1.0))
        assertEquals(2, grid.beatAt(1.49))
        assertEquals(grid.beatCount - 1, grid.beatAt(1000.0), "Times after the song belong to the last beat")
    }

    @Test
    fun `beats outside the grid are extrapolated at the nearest tempo`() {
        val grid = BeatGrid.uniform(0.5, 4, 4.0)

        assertEquals(-1.0, grid.timeOf(-2), TOLERANCE)
        assertEquals(grid.beats.last() + 1.5, grid.timeOf(grid.beatCount - 1 + 3), TOLERANCE)
    }

    @Test
    fun `the nearest bar line is found only within the tolerance`() {
        val grid = BeatGrid.uniform(0.5, 4, 20.0)

        assertEquals(4.0, grid.nearestBarLine(4.3, tolerance = 0.5)!!, TOLERANCE)
        assertNull(grid.nearestBarLine(5.0, tolerance = 0.5), "5 s is a whole second from the nearest bar line")
        assertEquals(5.0, grid.nearestBeat(5.1), TOLERANCE)
    }

    @Test
    fun `a sequence's tempo changes move its beats`() {
        val sequence = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120, absoluteTime = 0)
                tempo(60, absoluteTime = 4 * TPQ)
                note(60, duration = TPQ, absoluteTime = 7 * TPQ)
            }
        }.toTimeBasedSequence()

        val grid = BeatGrid.from(sequence)

        // Four beats at 120 BPM take two seconds; after that, each beat takes a full second.
        assertEquals(2.0, grid.timeOf(4), TOLERANCE)
        assertEquals(3.0, grid.timeOf(5), TOLERANCE)
        assertTrue(grid.isBarStart(4), "Without a time signature, the song is in 4/4")
    }

    @Test
    fun `a sequence's time signature sets where its bars begin`() {
        val sequence = smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(TPQ)
            track {
                tempo(120, absoluteTime = 0)
                timeSignature(numerator = 3, denominator = 2, metronome = 24, absoluteTime = 0)
                note(60, duration = TPQ, absoluteTime = 11 * TPQ)
            }
        }.toTimeBasedSequence()

        val grid = BeatGrid.from(sequence)

        assertEquals(listOf(0, 3, 6, 9), grid.barStartBeats.take(4), "In 3/4, a bar begins every three beats")
        assertEquals(3, grid.beatsPerBarAt(4))
    }

    private companion object {
        const val TPQ = 480
        const val TOLERANCE = 1e-6
    }
}
