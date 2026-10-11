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
import org.wysko.midis2jam2.domain.GervillMidiDevice
import org.wysko.midis2jam2.midi.system.JwSequencerImpl
import org.wysko.midis2jam2.midi.system.MidiPorts
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.NoOpMidiDevice
import org.wysko.midis2jam2.testing.Spec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Playing a file whose tracks are spread over more than one MIDI port (issue #227).
 *
 * Each port has 16 channels of its own, so a piano on channel 1 of port 1 and an alto sax on channel 1 of port 2 have
 * to reach different synthesizers to both be heard. Played on one, the last program change wins, and the piano part
 * sounds like a saxophone.
 */
class MultiPortPlaybackTest {

    private val piano = 0
    private val altoSax = 65

    @Test
    @Spec("midi.ports.playback.gervill")
    fun `each MIDI port's channels reach a device of their own`() {
        val first = NoOpMidiDevice("Port 1")
        val second = NoOpMidiDevice("Port 2")

        seekPartWay(MidiFixtures.twoPorts(first = piano, second = altoSax), MidiPorts(listOf(first, second)))

        assertEquals(listOf("programChange ch=0 program=$piano"), first.programChanges(), "Port 1's device")
        assertEquals(listOf("programChange ch=0 program=$altoSax"), second.programChanges(), "Port 2's device")
    }

    @Test
    @Spec("midi.ports.playback.external")
    fun `a single device plays every MIDI port on its own 16 channels`() {
        val device = NoOpMidiDevice()

        seekPartWay(MidiFixtures.twoPorts(first = piano, second = altoSax), MidiPorts.of(device))

        assertEquals(
            listOf("programChange ch=0 program=$altoSax"),
            device.programChanges(),
            "Both ports' channel 1 is the device's channel 1, so the last program change on either is in effect",
        )
    }

    @Test
    @Spec("midi.ports.playback.gervill")
    fun `Gervill plays each MIDI port on a synthesizer of its own`() {
        val ports = MidiPackage.gervillPorts(portCount = 3)

        assertEquals(3, ports.distinctDevices.size, "Three ports need three synthesizers, got ${ports.devices}")
        assertSame(GervillMidiDevice.instance, ports[0], "The first port plays on the app's own Gervill")
        assertTrue(ports.devices.all { it is GervillMidiDevice })
    }

    /** Plays [file] on [ports] from part way through, which sends each channel's state to its port's device. */
    private fun seekPartWay(file: TimeBasedSequence, ports: MidiPorts) {
        val sequencer = JwSequencerImpl().apply {
            open(ports)
            sequence = file
        }
        try {
            val chased = CountDownLatch(1)
            sequencer.setPosition(1.seconds, start = false) { chased.countDown() }
            assertTrue(chased.await(10, TimeUnit.SECONDS), "The sequencer never finished seeking")
        } finally {
            sequencer.close()
        }
    }

    private fun NoOpMidiDevice.programChanges() = messages.filter { it.startsWith("programChange") }
}
