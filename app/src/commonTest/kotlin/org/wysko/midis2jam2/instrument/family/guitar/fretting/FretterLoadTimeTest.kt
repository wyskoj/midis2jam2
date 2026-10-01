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

package org.wysko.midis2jam2.instrument.family.guitar.fretting

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * Keeps the fretting engine quick on the parts that cost it the most.
 *
 * Every fretted instrument is solved while the song loads, so a slow solve is a slow start. The worst inputs are
 * General MIDI guitar tracks that were written on a keyboard: thick voicings with more notes than strings, each of
 * which makes the engine search for the notes to leave out, and very long, fast lines. The limits are generous; they
 * catch a solve that has become many times slower, not ordinary variation between machines.
 */
class FretterLoadTimeTest {
    private val clean = FrettingProfiles.guitar(GuitarStyle.CLEAN)

    @Test
    fun `a long part of keyboard voicings with more notes than strings solves quickly`() {
        val random = Random(1)
        val notes = (0 until 2000).flatMap { i ->
            val root = 36 + random.nextInt(12)
            val voicing = listOf(0, 7, 12, 16, 19, 24, 28, 31, 36, 40).take(8 + random.nextInt(3))
            voicing.map { FrettingNote(root + it, i * 0.25, i * 0.25 + 0.24) }
        }
        assertSolvesWithin(notes, LIMIT_SECONDS)
    }

    @Test
    fun `a ten-thousand-note fast line solves quickly`() {
        val random = Random(2)
        var pitch = 60
        val notes = (0 until 10_000).map { i ->
            pitch = (pitch + random.nextInt(-4, 5)).coerceIn(45, 84)
            FrettingNote(pitch, i * 0.06, i * 0.06 + 0.05)
        }
        assertSolvesWithin(notes, LIMIT_SECONDS)
    }

    private fun assertSolvesWithin(notes: List<FrettingNote>, seconds: Int) {
        val (solution, took) = measureTimedValue { Fretter.solve(notes, clean) }
        println("Solved ${notes.size} notes in $took")
        assertTrue(solution.fingerings.any { it != null }, "Nothing was fingered")
        assertTrue(took < seconds.seconds, "Solving ${notes.size} notes took $took; the limit is $seconds s")
    }

    private companion object {
        const val LIMIT_SECONDS = 10
    }
}
