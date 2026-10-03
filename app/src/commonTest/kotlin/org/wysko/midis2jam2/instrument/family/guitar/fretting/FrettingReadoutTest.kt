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

import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.melody
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.progression
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The live fretting readout's text: what it says about the whole part, and about the moment it's showing.
 */
class FrettingReadoutTest {

    private val riff = listOf(listOf(38, 45, 50), listOf(38, 45, 50), listOf(41, 48, 53), listOf(43, 50, 55))
    private val notes = progression(List(4) { riff }.flatten(), 0.5) +
        melody(List(4) { listOf(69, 72, 74, 76, 74, 72, 69, 67) }.flatten(), 0.125, start = 8.0)
    private val solution = Fretter.solve(notes, FrettingProfiles.guitar(GuitarStyle.DRIVEN))

    @Test
    @Spec("app.debug.fretting-readout")
    fun `the readout names the inferred tuning and whether the moment is rhythm or lead`() {
        val duringChords = FrettingReadout.format(solution, 1.1)
        val duringSolo = FrettingReadout.format(solution, 11.1)
        listOf(duringChords, duringSolo).forEach { assertTrue("Drop D" in it, "The readout should name the tuning:\n$it") }
        assertTrue("RHYTHM" in duringChords, "Chords should read as rhythm:\n$duringChords")
        assertTrue("LEAD" in duringSolo, "The solo should read as lead:\n$duringSolo")
    }

    @Test
    fun `the readout shows the frets being played`() {
        // At 1.1 s the third power chord of the riff sounds: F on the low D string, third fret, shape 3-3-3.
        val index = FrettingReadout.currentSlice(solution.slices, 1.1)
        val slice = solution.slices[index]
        val text = FrettingReadout.format(solution, 1.1)
        val fretLine = text.lines().first { it.trimStart().startsWith("fret") }
        slice.frets.filter { it >= 0 }.forEach { fret ->
            assertTrue(fretLine.split(" ").contains(fret.toString()), "Fret $fret missing from \"$fretLine\"")
        }
    }

    @Test
    fun `the readout copes with times before the first note and after the last`() {
        assertTrue("before the first note" in FrettingReadout.format(solution, -1.0))
        assertTrue("(ended)" in FrettingReadout.format(solution, 1000.0))
        FrettingReadout.format(Fretter.solve(emptyList(), FrettingProfiles.bass(BassStyle.STANDARD)), 0.0)
    }
}
