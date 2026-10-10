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

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.reed.sax.AltoSax
import org.wysko.midis2jam2.instrument.family.strings.AcousticBass
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Files whose tracks are spread over more than one MIDI port (issue #227).
 *
 * MuseScore writes a score with more than 16 instruments by putting tracks on different ports with MIDI Port meta
 * events, each port having 16 channels of its own. Before ports were read, two instruments on the same channel number
 * of different ports were merged: a piano on port 1 and an alto sax on port 2, both on channel 1, came out as one
 * instrument playing both parts.
 */
class MultiPortAssignmentTest {

    private val piano = 0
    private val altoSax = 65
    private val jazzKit = 32

    @Test
    @Spec("midi.ports.channels")
    fun `the same channel on two MIDI ports plays two instruments`() {
        val instruments = assign(MidiFixtures.twoPorts(first = piano, second = altoSax))

        assertEquals(1, instruments.count { it is Keyboard }, "Port 1's piano, got $instruments")
        assertEquals(1, instruments.count { it is AltoSax }, "Port 2's alto sax, got $instruments")
    }

    @Test
    @Spec("midi.ports.channels")
    fun `the same program on the same channel of two MIDI ports is two instruments`() {
        val instruments = assign(MidiFixtures.twoPorts(first = piano, second = piano))

        assertEquals(
            2,
            instruments.count { it is Keyboard },
            "Each port's channel 1 is its own piano, but got $instruments",
        )
    }

    @Test
    @Spec("midi.ports.drums")
    fun `channel 10 of a second MIDI port plays drums`() {
        val instruments = assign(MidiFixtures.drumsOnSecondPort())

        assertTrue(instruments.any { it is DrumSet }, "Port 2's channel 10 is a rhythm channel, got $instruments")
        assertTrue(instruments.any { it is Keyboard }, "Port 1's piano is missing from $instruments")
    }

    @Test
    @Spec("midi.ports.sysex-scope")
    fun `a reset sent to one MIDI port doesn't undo another port's rhythm part`() {
        val instruments = assign(MidiFixtures.resetOnSecondPort(kit = jazzKit))

        assertTrue(
            instruments.none { it is AcousticBass },
            "Port 2's GS reset reached port 1, turning channel 11's Jazz kit into an acoustic bass: $instruments",
        )
        assertTrue(instruments.any { it is DrumSet }, "Port 1's channel 11 is a rhythm part, got $instruments")
    }

    private fun assign(file: TimeBasedSequence) =
        HeadlessPerformance.start(MidiFixtures.empty(), attachManagers = false).use { it.assignFor(file) }
}
