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

package org.wysko.midis2jam2.performance

import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.FretboardPosition
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fretted instruments in a real (headless) performance: MIDI files built the way recorded ones are, played through
 * the actual instrument assignment and fretting engine, with the positions the instruments will draw.
 */
class FrettedInstrumentTest {

    @Test
    @Spec(
        "instrument.fretted.every-note-shown",
        "instrument.fretted.one-note-per-string",
        "instrument.fretted.overlapping-chords",
    )
    fun `a humanized strummed guitar part puts every chord on its own strings`() {
        HeadlessPerformance.start(MidiFixtures.humanizedGuitarChords(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            val positions = guitar.notePeriodFretboardPosition

            val missing = guitar.timedArcs.filter { it !in positions }
            assertTrue(missing.isEmpty(), "These notes have no place on the fretboard: ${missing.map { it.note }}")

            // The chords ring a few ticks into each other; each must still be fingered as its own chord.
            val chords = guitar.timedArcs.groupBy { it.start / (2 * MidiFixtures.TICKS_PER_QUARTER) }.toSortedMap()
            assertEquals(MidiFixtures.OPEN_CHORDS.size * 2, chords.size)
            chords.values.forEach { arcs ->
                val strings = arcs.map { positions.getValue(it).string }
                assertEquals(strings.size, strings.toSet().size, "A chord's notes share a string: ${describe(arcs, positions)}")
            }
            val openE = chords.values.first().sortedBy { it.note }.map { positions.getValue(it) }
            assertEquals(
                listOf(0 to 0, 1 to 2, 2 to 2, 3 to 1, 4 to 0, 5 to 0),
                openE.map { it.string to it.fret },
                "The first chord should be the open E shape",
            )
        }
    }

    @Test
    @Spec("instrument.guitar.tuning")
    fun `a drop-D riff is played in drop D`() {
        HeadlessPerformance.start(MidiFixtures.dropDRiff(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            assertEquals("drop-d", guitar.fretting.tuning.id)
            assertTrue(guitar.timedArcs.all { it in guitar.notePeriodFretboardPosition }, "Every note of the riff is playable in drop D")
        }
    }

    @Test
    @Spec("instrument.guitar.bends-fretted")
    fun `the bent note of a solo is fretted, with room to bend`() {
        HeadlessPerformance.start(MidiFixtures.guitarSoloWithBends(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            val bent = guitar.timedArcs.sortedBy { it.start }[MidiFixtures.BENT_NOTE_INDEX]
            val position = guitar.notePeriodFretboardPosition.getValue(bent)
            assertTrue(position.fret > 0, "The bent note is on an open string, which can't be bent: $position")
            assertTrue(position.fret + 2 <= guitar.fretting.profile.fretCount, "No room for a whole-step bend: $position")
        }
    }

    @Test
    @Spec("instrument.guitar.rhythm-and-lead")
    fun `a guitar that strums and then solos reads as rhythm and then as lead`() {
        HeadlessPerformance.start(MidiFixtures.rhythmThenSolo(), attachManagers = false).use { performance ->
            val guitar = performance.instruments.filterIsInstance<Guitar>().single()
            val slices = guitar.fretting.solution.slices
            val chords = slices.filter { it.start < MidiFixtures.SOLO_START_SECONDS - 2.0 }
            val solo = slices.filter { it.start > MidiFixtures.SOLO_START_SECONDS + 1.5 }
            assertTrue(chords.isNotEmpty() && solo.isNotEmpty())
            assertTrue(chords.all { it.leadness < 0.3 }, "The chords should read as rhythm: ${chords.map { it.leadness }}")
            assertTrue(solo.all { it.leadness > 0.7 }, "The solo should read as lead: ${solo.map { it.leadness }}")
        }
    }

    @Test
    @Spec("instrument.bass.tuning")
    fun `a bass line is played in standard tuning with its octaves in the octave shape`() {
        HeadlessPerformance.start(MidiFixtures.bassLine(), attachManagers = false).use { performance ->
            val bass = performance.instruments.filterIsInstance<BassGuitar>().single()
            assertEquals("e-standard", bass.fretting.tuning.id)
            val octaves = bass.timedArcs.filter { it.note.toInt() == 40 }.map { bass.notePeriodFretboardPosition.getValue(it) }
            assertTrue(octaves.isNotEmpty())
            assertTrue(octaves.all { it == FretboardPosition(2, 2) }, "E2 should sit an octave shape above the open E: $octaves")
        }
    }

    private fun describe(arcs: List<org.wysko.kmidi.midi.TimedArc>, positions: Map<org.wysko.kmidi.midi.TimedArc, FretboardPosition>) =
        arcs.joinToString { "${it.note}=${positions[it]}" }
}
