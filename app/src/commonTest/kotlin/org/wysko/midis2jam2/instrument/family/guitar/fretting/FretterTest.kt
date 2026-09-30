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

import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.chord
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.describe
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.melody
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.progression
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * How the fretting engine fingers whole parts: chords, riffs, bass lines, and the rules that hold for every part.
 *
 * The engine replaced one that merged slightly overlapping chords into nonsense and let the hand jump around the
 * neck; these tests hold it to fingering parts the way a player would.
 */
class FretterTest {

    private val clean = FrettingProfiles.guitar(GuitarStyle.CLEAN)
    private val driven = FrettingProfiles.guitar(GuitarStyle.DRIVEN)

    private val openChords = listOf(
        listOf(40, 47, 52, 56, 59, 64), // E
        listOf(45, 52, 57, 61, 64), // A
        listOf(50, 57, 62, 66), // D
        listOf(43, 47, 50, 55, 59, 67), // G
    )

    @Test
    @Spec("instrument.fretted.overlapping-chords")
    fun `chords that ring slightly into the next chord are fingered as separate chords`() {
        val tidy = Fretter.solve(progression(openChords, 1.0), clean)
        val late = Fretter.solve(progression(openChords, 1.0, lateOff = 0.03), clean)
        val strummed = Fretter.solve(progression(openChords, 1.0, spread = 0.015, lateOff = 0.05), clean)

        listOf(tidy, late, strummed).forEach { assertEquals(4, it.global.sliceCount, "Four chords, four slices") }
        assertEquals(tidy.fingerings, late.fingerings, "Late note-offs changed the fingering")
        assertEquals(tidy.fingerings, strummed.fingerings, "Strumming changed the fingering")
        assertEquals(
            listOf(0 to 0, 1 to 2, 2 to 2, 3 to 1, 4 to 0, 5 to 0),
            tidy.fingerings.take(6).map { it!!.string to it.fret },
            "The E chord should be the open E shape",
        )
    }

    @Test
    @Spec("instrument.fretted.every-note-shown")
    fun `every note that can be played is given a place`() {
        val notes = progression(openChords, 0.5) + melody(listOf(64, 67, 69, 71, 72, 74, 76), 0.2, start = 2.0)
        val solution = Fretter.solve(notes, clean)
        assertTrue(solution.fingerings.all { it != null }, "Unplaced notes: ${solution.describe(notes)}")
        assertEquals(0, solution.global.droppedCount)
    }

    @Test
    fun `a chord with more notes than strings loses only a doubling`() {
        val notes = chord(0.0, 1.0, listOf(40, 47, 52, 56, 59, 64, 68))
        val solution = Fretter.solve(notes, clean)
        assertEquals(1, solution.fingerings.count { it == null }, solution.describe(notes))
    }

    @Test
    @Spec("instrument.fretted.one-note-per-string")
    fun `notes that ring together are on different strings`() {
        // A bass note held under a melody (E3 could go on the A string, but that string is ringing), then chords
        // whose notes ring slightly into the next.
        val notes = listOf(FrettingNote(45, 0.0, 2.0)) + melody(listOf(52, 55, 57, 60, 59, 57), 0.25, start = 0.25) +
            progression(openChords, 0.5, start = 2.0, lateOff = 0.02)
        val solution = Fretter.solve(notes, clean)
        for (i in notes.indices) {
            for (j in i + 1 until notes.size) {
                val overlap = minOf(notes[i].end, notes[j].end) - maxOf(notes[i].start, notes[j].start)
                if (overlap <= SLOP) continue
                val a = assertNotNull(solution.fingerings[i])
                val b = assertNotNull(solution.fingerings[j])
                assertTrue(
                    a.string != b.string,
                    "${noteName(notes[i].pitch)} and ${noteName(notes[j].pitch)} ring together on string ${a.string}: " +
                        solution.describe(notes),
                )
            }
        }
        assertEquals(0, solution.global.stealCount, "No ringing note should be cut short: ${solution.describe(notes)}")
    }

    @Test
    @Spec("instrument.fretted.stays-in-position")
    fun `a lick that fits in one position stays there`() {
        val lick = melody(listOf(57, 60, 62, 64, 67, 64, 62, 60, 57, 60, 62, 64), 0.15)
        val solution = Fretter.solve(lick, driven)
        val hands = solution.slices.map { it.hand }.filter { it >= 0 }
        assertTrue(hands.max() - hands.min() <= 2, "The hand moved between frets $hands: ${solution.describe(lick)}")
    }

    @Test
    fun `a power-chord riff slides one shape along the same strings`() {
        val e5 = listOf(40, 47, 52)
        val g5 = listOf(43, 50, 55)
        val a5 = listOf(45, 52, 57)
        val notes = progression(listOf(e5, g5, a5, g5, e5, g5, a5, g5), 0.5)
        val solution = Fretter.solve(notes, driven)
        notes.indices.chunked(3).forEach { chordNotes ->
            assertEquals(listOf(0, 1, 2), chordNotes.map { solution.fingerings[it]!!.string }, solution.describe(notes))
        }
    }

    @Test
    fun `a bass line jumping an octave uses the octave shape`() {
        val notes = melody(listOf(28, 40, 28, 40, 33, 45, 33, 45), 0.25)
        val solution = Fretter.solve(notes, FrettingProfiles.bass(BassStyle.STANDARD))
        assertEquals(2 to 2, solution.fingerings[1]!!.let { it.string to it.fret }, "E2 should be the octave above the open E")
        assertEquals(3 to 2, solution.fingerings[5]!!.let { it.string to it.fret }, "A2 should be the octave above the open A")
    }

    @Test
    fun `a violin scale is played in first position`() {
        val scale = melody(listOf(55, 57, 59, 60, 62, 64, 66, 67, 69, 71, 72, 74, 76), 0.25)
        val solution = Fretter.solve(scale, FrettingProfiles.violin())
        assertTrue(solution.fingerings.all { it!!.fret <= 5 }, solution.describe(scale))
    }

    @Test
    @Spec("instrument.fretted.riffs-consistent")
    fun `a repeated riff is fingered the same way every time`() {
        val riff = listOf(52, 55, 57, 59, 57, 55, 52, 50)
        val notes = melody(List(4) { riff }.flatten(), 0.125)
        val solution = Fretter.solve(notes, driven)
        val occurrences = notes.indices.chunked(riff.size).map { indices -> indices.map { solution.fingerings[it] } }
        occurrences.drop(1).forEach { assertEquals(occurrences.first(), it, solution.describe(notes)) }
    }

    @Test
    fun `the same part always gets the same fingering`() {
        val notes = progression(openChords, 0.5) + melody(listOf(69, 72, 74, 76, 74, 72, 69, 67), 0.125, start = 2.0)
        assertEquals(Fretter.solve(notes, driven).fingerings, Fretter.solve(notes, driven).fingerings)
    }

    @Test
    fun `an empty part solves to nothing`() {
        val solution = Fretter.solve(emptyList(), clean)
        assertTrue(solution.fingerings.isEmpty())
        assertEquals(clean.defaultTuning, solution.tuning)
    }

    private companion object {
        /** Overlaps this short are MIDI timing slop, and a new note may take the string. */
        const val SLOP = 0.06
    }
}
