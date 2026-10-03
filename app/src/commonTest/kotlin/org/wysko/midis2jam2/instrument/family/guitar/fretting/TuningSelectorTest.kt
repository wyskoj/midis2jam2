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
import kotlin.test.assertEquals

/**
 * Inferring the tuning and capo from the music.
 *
 * The old engine switched to drop D whenever a single note fell below the low E, and couldn't show anything lower
 * than D at all. The tuning should follow from how the whole part plays, with the common tunings favoured.
 */
class TuningSelectorTest {

    private val openChords = listOf(
        listOf(40, 47, 52, 56, 59, 64),
        listOf(45, 52, 57, 61, 64),
        listOf(50, 57, 62, 66),
        listOf(43, 47, 50, 55, 59, 67),
    )

    @Test
    @Spec("instrument.guitar.tuning")
    fun `ordinary open chords are played in standard tuning without a capo`() {
        val solution = Fretter.solve(progression(openChords + openChords, 1.0), FrettingProfiles.guitar(GuitarStyle.ACOUSTIC))
        assertEquals("e-standard" to 0, solution.tuning.id to solution.capo)
    }

    @Test
    @Spec("instrument.guitar.tuning")
    fun `a riff built on a low D picks drop D`() {
        val riff = listOf(listOf(38, 45, 50), listOf(38, 45, 50), listOf(41, 48, 53), listOf(43, 50, 55), listOf(38, 45, 50))
        val solution = Fretter.solve(progression(List(4) { riff }.flatten(), 0.25), FrettingProfiles.guitar(GuitarStyle.DRIVEN))
        assertEquals("drop-d", solution.tuning.id)
        assertEquals(0, solution.global.droppedCount)
    }

    @Test
    @Spec("instrument.guitar.tuning")
    fun `open chords a semitone down pick E-flat tuning`() {
        val lowered = List(3) { openChords }.flatten().map { chord -> chord.map { it - 1 } }
        val solution = Fretter.solve(progression(lowered, 1.0), FrettingProfiles.guitar(GuitarStyle.ACOUSTIC))
        assertEquals("eb-standard", solution.tuning.id)
    }

    @Test
    @Spec("instrument.guitar.tuning")
    fun `one stray low note doesn't retune the guitar`() {
        val notes = progression(List(3) { openChords }.flatten(), 1.0) + FrettingNote(38, 12.5, 12.7)
        val solution = Fretter.solve(notes, FrettingProfiles.guitar(GuitarStyle.CLEAN))
        assertEquals("e-standard", solution.tuning.id)
        assertEquals(1, solution.global.droppedCount, "Only the stray note is left out")
    }

    @Test
    @Spec("instrument.guitar.tuning")
    fun `open-chord shapes moved up two frets pick a capo on the second fret`() {
        val shapes = listOf(
            listOf(43, 47, 50, 55, 59, 67), // G
            listOf(48, 52, 55, 60, 64), // C
            listOf(50, 57, 62, 66), // D
            listOf(40, 47, 52, 55, 59, 64), // Em
        )
        val song = List(6) { shapes }.flatten().map { chord -> chord.map { it + 2 } }
        val solution = Fretter.solve(progression(song, 1.0, spread = 0.01), FrettingProfiles.guitar(GuitarStyle.ACOUSTIC))
        assertEquals("e-standard" to 2, solution.tuning.id to solution.capo)
        assertEquals(listOf(5, 4, 2, 2, 2, 5), solution.fingerings.take(6).map { it!!.fret }, "The G shape, behind a capo at 2")
    }

    @Test
    @Spec("instrument.bass.tuning")
    fun `a bass line that keeps hitting a low D picks drop D, and an ordinary one standard`() {
        val bass = FrettingProfiles.bass(BassStyle.STANDARD)
        val dropped = melody(List(6) { listOf(26, 26, 38, 26, 29, 31, 33, 26) }.flatten(), 0.25)
        assertEquals("drop-d", Fretter.solve(dropped, bass).tuning.id)
        val ordinary = melody(List(6) { listOf(28, 28, 40, 28, 31, 33, 35, 36) }.flatten(), 0.25)
        assertEquals("e-standard", Fretter.solve(ordinary, bass).tuning.id)
    }

    @Test
    fun `a given tuning is used as is`() {
        val dadgad = Tunings.GUITAR.first { it.id == "dadgad" }
        val solution = Fretter.solve(progression(openChords, 1.0), FrettingProfiles.guitar(GuitarStyle.CLEAN), tuning = dadgad)
        assertEquals(dadgad, solution.tuning)
    }
}
