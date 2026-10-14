/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.testing.MidiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Which of the two ride cymbals each ride note is played on. When the ride and its bell are struck together, the smart
 * drum sticks would otherwise send both sticks to one cymbal; instead the ride hit moves to the other ride, the way a
 * drummer with two rides would play it.
 */
class RideCymbalPartitionTest {

    private val notes = MidiFixtures.rideBellGroove().smf.tracks.flatMap { it.events }
        .filterIsInstance<NoteEvent.NoteOn>()

    @Test
    fun `a ride hit struck with the bell moves to the other ride`() {
        val (ride1, ride2) = DrumSet.partitionRideCymbals(notes, splitBellChords = true)
        val bellTicks = notes.filter { it.note == RIDE_BELL }.map { it.tick }.toSet()

        assertEquals(bellTicks, ride2.map { it.tick }.toSet(), "The ride hits struck with the bell")
        assertTrue(ride2.all { it.note == RIDE_CYMBAL_1 }, "Only the ride hits move, not the bell: $ride2")
        assertEquals(notes.size - bellTicks.size, ride1.size, "The bell and the other ride hits stay on the first ride")
        assertTrue(ride1.count { it.note == RIDE_BELL } == bellTicks.size)
    }

    @Test
    fun `without splitting, everything stays on the first ride`() {
        val (ride1, ride2) = DrumSet.partitionRideCymbals(notes)
        assertEquals(notes.size, ride1.size)
        assertTrue(ride2.isEmpty())
    }
}
