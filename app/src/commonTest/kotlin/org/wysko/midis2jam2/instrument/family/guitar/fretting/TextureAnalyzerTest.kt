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

import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.describe
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.melody
import org.wysko.midis2jam2.instrument.family.guitar.fretting.FrettingTestNotes.progression
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Telling rhythm playing from lead playing, and finding where phrases start.
 *
 * One guitar track often strums a verse and then takes a solo; each needs to be fingered its own way.
 */
class TextureAnalyzerTest {

    private val chords = listOf(listOf(40, 47, 52, 56, 59, 64), listOf(45, 52, 57, 61, 64), listOf(50, 57, 62, 66), listOf(43, 47, 50, 55, 59, 67))
    private val rhythmThenSolo =
        progression(List(4) { chords }.flatten(), 0.5) +
            melody(List(4) { listOf(69, 72, 74, 76, 74, 72, 69, 67) }.flatten(), 0.125, start = 8.0)

    @Test
    @Spec("instrument.guitar.rhythm-and-lead")
    fun `strummed chords read as rhythm and a single-note solo as lead`() {
        val slices = OnsetSlicer.slice(rhythmThenSolo, 6)
        val texture = TextureAnalyzer.analyze(rhythmThenSolo, slices)
        slices.forEachIndexed { i, slice ->
            when {
                slice.time < 6.0 -> assertTrue(texture.leadness[i] < 0.3, "Chords at ${slice.time}s read as ${texture.leadness[i]}")
                slice.time >= 10.0 -> assertTrue(texture.leadness[i] > 0.7, "Solo at ${slice.time}s reads as ${texture.leadness[i]}")
            }
        }
    }

    @Test
    @Spec("instrument.guitar.rhythm-and-lead")
    fun `the chords get chord shapes and the solo stays in a box`() {
        val solution = Fretter.solve(rhythmThenSolo, FrettingProfiles.guitar(GuitarStyle.DRIVEN))
        val chordNotes = rhythmThenSolo.indices.filter { rhythmThenSolo[it].start < 8.0 }
        assertTrue(chordNotes.all { solution.fingerings[it] != null }, solution.describe(rhythmThenSolo))
        val soloHands = solution.slices.filter { it.start >= 8.5 }.map { it.hand }
        assertTrue(soloHands.max() - soloHands.min() <= 2, "The solo wandered between frets $soloHands")
    }

    @Test
    fun `a let-ring arpeggio reads as rhythm`() {
        val arpeggio = listOf(45, 52, 57, 60, 64, 60, 57, 52).mapIndexed { i, p -> FrettingNote(p, i * 0.2, 1.6) }
        val slices = OnsetSlicer.slice(arpeggio, 6)
        val texture = TextureAnalyzer.analyze(arpeggio, slices)
        assertTrue(texture.leadness.all { it < 0.5 }, "Ringing arpeggio notes are chord playing: ${texture.leadness.toList()}")
    }

    @Test
    fun `a rest ends a phrase`() {
        val notes = melody(listOf(60, 62, 64, 65, 67), 0.15) + melody(listOf(69, 71, 72), 0.15, start = 1.5)
        val slices = OnsetSlicer.slice(notes, 6)
        val texture = TextureAnalyzer.analyze(notes, slices)
        assertEquals(listOf(true, false, false, false, false, true, false, false), texture.phraseStart.toList())
    }
}
