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
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Listing the ways a chord or note can be fingered.
 *
 * Everything the decoder can choose comes from here, so every candidate must be physically playable: one note per
 * string, on the neck, within the hand's reach.
 */
class CandidateGeneratorTest {

    private val guitar = FrettingProfiles.guitar(GuitarStyle.CLEAN)

    @Test
    fun `the open E major shape is offered for an E major chord`() {
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(40, 47, 52, 56, 59, 64)), guitar)
        val shapes = candidates.map { c -> c.strings.indices.associate { c.strings[it] to c.frets[it] } }
        assertTrue(
            shapes.contains(mapOf(0 to 0, 1 to 2, 2 to 2, 3 to 1, 4 to 0, 5 to 0)),
            "Expected the open E shape (022100) among $shapes",
        )
    }

    @Test
    fun `every candidate uses each string once and stays on the neck`() {
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(48, 52, 55, 60, 64)), guitar)
        assertTrue(candidates.isNotEmpty())
        candidates.forEach { c ->
            assertEquals(c.strings.size, c.strings.toSet().size, "Two notes share a string: ${c.strings.contentToString()}")
            assertTrue(c.frets.all { it in 0..guitar.fretCount }, "Off the neck: ${c.frets.contentToString()}")
        }
    }

    @Test
    fun `fingerings wider than the hand can stretch are not offered`() {
        // F2 can only be played at the first fret; A4 anywhere from the fifth fret up.
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(41, 69)), guitar)
        assertTrue(candidates.isNotEmpty())
        candidates.forEach { c ->
            assertTrue(c.spanMm <= guitar.maxSpanMm, "A ${c.spanMm} mm stretch was offered: ${c.frets.contentToString()}")
        }
    }

    @Test
    fun `a chord with more notes than strings leaves out an octave doubling`() {
        val pitches = listOf(40, 47, 52, 56, 59, 64, 68)
        val notes = chord(0.0, 1.0, pitches)
        val candidates = candidatesFor(notes, guitar)
        candidates.forEach { assertEquals(1, it.dropped.size, "Exactly one note has to go") }
        val best = candidates.first()
        val dropped = notes[best.dropped.single()].pitch
        val kept = best.notes.map { notes[it].pitch }
        assertTrue(kept.any { it.mod(12) == dropped.mod(12) }, "Dropped ${noteName(dropped)}, which isn't doubled in $kept")
        assertTrue(40 in kept && 68 in kept, "The bass and the top note should be kept: $kept")
    }

    @Test
    fun `bowed double stops are on adjacent strings`() {
        val violin = FrettingProfiles.violin()
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(55, 76)), violin)
        assertTrue(candidates.isNotEmpty())
        candidates.forEach { c ->
            val strings = c.strings.sorted()
            assertEquals(1, strings.last() - strings.first(), "Not adjacent: $strings")
        }
    }

    @Test
    @Spec("instrument.guitar.harmonics")
    fun `guitar harmonics are offered at the natural harmonic frets`() {
        val harmonics = FrettingProfiles.guitar(GuitarStyle.HARMONICS)
        // E3 is the low E string's octave harmonic, at the twelfth fret.
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(52)), harmonics)
        assertTrue(
            candidates.any { it.harmonic.single() && it.strings.single() == 0 && it.frets.single() == 12 },
            "The twelfth-fret harmonic of the low E string should be offered",
        )
        assertTrue(candidatesFor(chord(0.0, 1.0, listOf(52)), guitar).none { it.harmonic.any { h -> h } })

        val solution = Fretter.solve(chord(0.0, 1.0, listOf(52)) + chord(1.0, 1.0, listOf(59)), harmonics)
        solution.fingerings.forEach { f ->
            assertTrue(f!!.isHarmonic && f.fret in setOf(5, 7, 12), "Guitar Harmonics should play harmonics, got $f")
        }
    }

    @Test
    fun `fingers follow the frets from the index finger`() {
        val candidates = candidatesFor(chord(0.0, 1.0, listOf(45, 52, 57, 61, 64)), guitar)
        val openA = candidates.first { c -> c.strings.contentEquals(intArrayOf(1, 2, 3, 4, 5)) && c.frets.contentEquals(intArrayOf(0, 2, 2, 2, 0)) }
        val fingers = CandidateGenerator.assignFingers(openA, openA.minFret, 0)
        assertEquals(0, fingers[0], "The open A string needs no finger")
        assertTrue(fingers.drop(1).take(3).all { it in 1..4 })
    }

    private fun candidatesFor(notes: List<FrettingNote>, profile: FrettingProfile): List<Candidate> {
        val slices = OnsetSlicer.slice(notes, profile.stringCount)
        val texture = TextureAnalyzer.analyze(notes, slices)
        val weights = slices.indices.map { FrettingWeights.blend(profile.rhythm, profile.lead, texture.leadness[it]) }
        val ctx = DecodeContext(notes, slices, profile, texture, weights, profile.defaultTuning, 0, maxCandidates = 1000)
        return ctx.candidates(0)
    }
}
