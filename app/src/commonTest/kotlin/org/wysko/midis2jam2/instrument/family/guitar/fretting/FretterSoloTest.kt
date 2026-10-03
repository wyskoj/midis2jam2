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
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How the fretting engine plays single-note lead lines.
 *
 * Solos are played differently from chords: a player stays in one box and crosses strings, keeps hammer-ons and
 * slides on one string, bends on the thin strings, and moves up or down the neck between phrases rather than in
 * the middle of one.
 */
class FretterSoloTest {

    private val driven = FrettingProfiles.guitar(GuitarStyle.DRIVEN)

    @Test
    @Spec("instrument.guitar.solo-box-position")
    fun `a pentatonic solo stays in one box by crossing strings`() {
        val run = melody(listOf(57, 60, 62, 64, 67, 69, 72, 74, 72, 69, 67, 64, 62, 60, 57), 0.12)
        val solution = Fretter.solve(run, driven)
        val hands = solution.slices.map { it.hand }
        assertTrue(hands.max() - hands.min() <= 1, "The hand moved between frets $hands: ${solution.describe(run)}")
        assertTrue(solution.fingerings.map { it!!.string }.toSet().size >= 4, "It should cross strings: ${solution.describe(run)}")
        assertTrue(solution.fingerings.none { it!!.fret == 0 }, "No open strings in a box: ${solution.describe(run)}")
    }

    @Test
    @Spec("instrument.guitar.solo-box-position")
    fun `a fast scale run crosses strings instead of sliding along one`() {
        val scale = listOf(52, 54, 56, 57, 59, 61, 63, 64, 66, 68, 69, 71, 73, 75, 76)
        val run = melody(scale, 0.08)
        val solution = Fretter.solve(run, driven)
        var longest = 1
        var current = 1
        for (i in 1 until run.size) {
            current = if (solution.fingerings[i]!!.string == solution.fingerings[i - 1]!!.string) current + 1 else 1
            longest = maxOf(longest, current)
        }
        assertTrue(longest <= 4, "$longest notes in a row on one string: ${solution.describe(run)}")
    }

    @Test
    @Spec("instrument.guitar.legato-same-string")
    fun `hammer-ons, pull-offs and trills stay on one string`() {
        val trill = melody(listOf(62, 64, 62, 64, 62, 64, 62, 64), 0.08, overlap = 0.005)
        val trillSolution = Fretter.solve(trill, driven)
        assertEquals(1, trillSolution.fingerings.map { it!!.string }.toSet().size, trillSolution.describe(trill))

        val hammerPull = melody(listOf(69, 72, 69, 72, 69), 0.1, overlap = 0.005)
        val hammerSolution = Fretter.solve(hammerPull, driven)
        assertEquals(1, hammerSolution.fingerings.map { it!!.string }.toSet().size, hammerSolution.describe(hammerPull))
    }

    @Test
    @Spec("instrument.guitar.bends-fretted")
    fun `bent notes are fretted, with room to bend, on the thin strings`() {
        // E4 and B3 are open strings in standard tuning, which can't be bent.
        val lick = melody(listOf(64, 62, 59, 57), 0.3, bends = mapOf(0 to 2.0, 2 to 1.0))
        val solution = Fretter.solve(lick, driven)
        listOf(0, 2).forEach { i ->
            val f = solution.fingerings[i]!!
            assertTrue(f.fret > 0, "A bent ${noteName(lick[i].pitch)} is on an open string: ${solution.describe(lick)}")
            assertTrue(f.fret + lick[i].bendUp <= driven.fretCount, "No room to bend: $f")
            assertTrue(f.string >= 2, "Nobody bends on the two lowest strings: ${solution.describe(lick)}")
        }
        assertTrue(solution.fingerings[0]!!.string in 3..5, "The big bend belongs on the G, B or high E: ${solution.describe(lick)}")
    }

    @Test
    @Spec("instrument.guitar.solo-box-position", "instrument.guitar.legato-same-string")
    fun `a solo whose notes overlap slightly, as sequenced MIDI often does, is fingered like a detached one`() {
        // Sequencers and notation software often let each note ring a few tens of milliseconds into the next.
        listOf(
            listOf(57, 60, 62, 64, 67, 69, 72, 74, 72, 69, 67, 64, 62, 60, 57) to 0.12,
            listOf(52, 54, 56, 57, 59, 61, 63, 64, 66, 68, 69, 71, 73, 75, 76) to 0.08,
        ).forEach { (pitches, step) ->
            val detached = melody(pitches, step)
            val legato = melody(pitches, step, overlap = 0.03)
            assertEquals(
                Fretter.solve(detached, driven).describe(detached),
                Fretter.solve(legato, driven).describe(legato),
                "Overlapping the notes by 30 ms changed the fingering",
            )
        }
    }

    @Test
    fun `a high phrase stays up the neck`() {
        val phrase = melody(listOf(76, 74, 71, 69, 67, 64), 0.15)
        val solution = Fretter.solve(phrase, driven)
        assertTrue(solution.fingerings.all { it!!.fret >= 9 }, "The phrase fell back to the nut: ${solution.describe(phrase)}")
        val hands = solution.slices.map { it.hand }
        assertTrue(hands.max() - hands.min() <= 3, "The hand moved between frets $hands")
    }

    @Test
    fun `a double stop at one fret on adjacent strings uses one finger`() {
        val notes = List(4) { chord(it * 0.25, 0.25, listOf(67, 72)) }.flatten()
        val solution = Fretter.solve(notes, driven)
        val (low, high) = solution.fingerings.take(2).map { it!! }
        assertEquals(1, high.string - low.string, solution.describe(notes))
        assertEquals(low.fret, high.fret, solution.describe(notes))
        assertEquals(low.finger, high.finger, "One finger across both strings: ${solution.describe(notes)}")
    }

    @Test
    @Spec("instrument.guitar.solo-box-position")
    fun `the hand moves up the neck between phrases, not during them`() {
        // A phrase on the low strings that only fits near the nut, then one that only fits high up.
        val low = melody(listOf(40, 42, 43, 45, 47, 45, 43, 42), 0.15)
        val high = melody(listOf(76, 74, 71, 69, 71, 74, 76, 74), 0.15, start = 2.0)
        val solution = Fretter.solve(low + high, driven)
        val lowHands = solution.slices.take(low.size).map { it.hand }.filter { it >= 0 }
        val highHands = solution.slices.drop(low.size).map { it.hand }
        assertTrue(lowHands.max() - lowHands.min() <= 2, "The first phrase wandered: $lowHands")
        assertTrue(highHands.max() - highHands.min() <= 2, "The second phrase wandered: $highHands")
        assertTrue(highHands.min() - lowHands.max() >= 5, "The second phrase should be well up the neck: $lowHands then $highHands")
        assertTrue(solution.slices[low.size].phraseStart, "The rest before the second phrase should end a phrase")
    }
}
