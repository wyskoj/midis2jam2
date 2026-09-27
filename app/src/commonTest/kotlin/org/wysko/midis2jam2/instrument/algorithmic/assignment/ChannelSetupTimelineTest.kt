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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState.Melody
import org.wysko.midis2jam2.instrument.algorithmic.assignment.ChannelState.Rhythm
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reading what each channel is set up to play (bank, program, melody or rhythm) out of a file's messages.
 *
 * General MIDI only has channel 10, but GS, XG and GM2 files can make any channel a rhythm channel. The synthesizer
 * plays those channels as drums, so if this reads them wrong, the stage shows a melodic instrument playing drum notes.
 * Bank select picks between variations of a program, but only from the next program change on.
 */
class ChannelSetupTimelineTest {

    @Test
    fun `with no messages, only channel 10 is a rhythm channel`() {
        val timeline = ChannelSetupTimeline.from(emptyList())

        repeat(16) { channel ->
            val expected = if (channel == 9) Rhythm else Melody
            assertEquals(expected, timeline.stateAt(channel, 0), "Channel ${channel + 1} at the start of the file")
            assertEquals(expected, timeline.stateAt(channel, 10_000), "Channel ${channel + 1} later in the file")
        }
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.gs")
    fun `the GS use-for-rhythm-part message makes a channel a rhythm channel, and can undo it`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GS_RESET),
                SysexEvent(100, gsUseForRhythmPart(part = 0x0A, value = 2)),
                SysexEvent(500, gsUseForRhythmPart(part = 0x0A, value = 0)),
            )
        )

        assertEquals(Melody, timeline.stateAt(10, 99), "Channel 11 before the message")
        assertEquals(Rhythm, timeline.stateAt(10, 100), "Channel 11 once it's set to rhythm map 2")
        assertEquals(Rhythm, timeline.stateAt(10, 499), "Channel 11 while it's still a rhythm part")
        assertEquals(Melody, timeline.stateAt(10, 500), "Channel 11 once it's set back to off")
        assertEquals(Rhythm, timeline.stateAt(9, 1000), "Channel 10 is untouched")
    }

    @Test
    fun `GS numbers its parts 10, then 1 to 9, then 11 to 16`() {
        val expectedChannel = mapOf(0x0 to 9, 0x1 to 0, 0x9 to 8, 0xA to 10, 0xF to 15)

        expectedChannel.forEach { (part, channel) ->
            // Channel 10 is already a rhythm part, so turn it off instead, to see that it moves.
            val value = if (channel == 9) 0 else 1
            val timeline = ChannelSetupTimeline.from(listOf(SysexEvent(0, gsUseForRhythmPart(part, value))))
            val expected = if (channel == 9) Melody else Rhythm

            assertEquals(expected, timeline.stateAt(channel, 0), "GS part $part should address channel ${channel + 1}")
        }
    }

    @Test
    fun `system exclusive data is understood with or without its F0 and F7`() {
        val bare = gsUseForRhythmPart(part = 0x0A, value = 1).drop(1).dropLast(1).toByteArray()
        val timeline = ChannelSetupTimeline.from(listOf(SysexEvent(0, bare)))

        assertEquals(Rhythm, timeline.stateAt(10, 0))
    }

    @Test
    fun `every kind of system reset puts channel 10 back as the only rhythm channel`() {
        listOf(GM_ON, GM2_ON, GS_RESET, XG_ON).forEach { reset ->
            val timeline = ChannelSetupTimeline.from(
                listOf(
                    SysexEvent(0, gsUseForRhythmPart(part = 0x0A, value = 1)),
                    SysexEvent(0, gsUseForRhythmPart(part = 0x00, value = 0)),
                    SysexEvent(100, reset),
                )
            )

            assertEquals(Rhythm, timeline.stateAt(10, 99), "Channel 11 before the reset")
            assertEquals(Melody, timeline.stateAt(10, 100), "Channel 11 after ${reset.hex()}")
            assertEquals(Rhythm, timeline.stateAt(9, 100), "Channel 10 after ${reset.hex()}")
        }
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.xg")
    fun `in XG, drum banks make rhythm channels and the part mode message does too`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, XG_ON),
                ControlChangeEvent(10, 3, 0, 127),
                ProgramEvent(10, 3, 0),
                ControlChangeEvent(20, 4, 0, 126),
                ProgramEvent(20, 4, 0),
                SysexEvent(30, xgPartMode(part = 5, mode = 2)),
                ControlChangeEvent(40, 3, 0, 0),
                ProgramEvent(40, 3, 0),
            )
        )

        assertEquals(Rhythm, timeline.stateAt(3, 10), "Bank 127 is a drum kit")
        assertEquals(Rhythm, timeline.stateAt(4, 20), "Bank 126 is the SFX kit")
        assertEquals(126, timeline.setupAt(4, 20).msb, "The SFX kit is chosen by its bank")
        assertEquals(Rhythm, timeline.stateAt(5, 30), "Part mode 2 is a drum setup")
        assertEquals(Melody, timeline.stateAt(3, 40), "Bank 0 is a normal voice again")
    }

    @Test
    fun `in XG, a normal bank on channel 10 does not take the drums away`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, XG_ON),
                ControlChangeEvent(10, 9, 0, 0),
                ProgramEvent(10, 9, 0),
            )
        )

        assertEquals(Rhythm, timeline.stateAt(9, 10))
    }

    @Test
    fun `bank 127 outside XG mode is not a drum kit`() {
        // GS uses bank 127 for the CM-64 sound set, which is melodic.
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GS_RESET),
                ControlChangeEvent(10, 3, 0, 127),
                ProgramEvent(10, 3, 0),
            )
        )

        assertEquals(Melody, timeline.stateAt(3, 10))
    }

    @Test
    @Spec("midi.assignment.rhythm-channel.gm2")
    fun `in GM2, bank 120 makes a rhythm channel and bank 121 a melodic one`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GM2_ON),
                ControlChangeEvent(10, 2, 0, 120),
                ProgramEvent(10, 2, 0),
                ControlChangeEvent(20, 9, 0, 121),
                ProgramEvent(20, 9, 0),
            )
        )

        assertEquals(Rhythm, timeline.stateAt(2, 10), "Bank 120 is a rhythm bank")
        assertEquals(Melody, timeline.stateAt(9, 20), "Bank 121 is a melody bank, even on channel 10")
    }

    @Test
    fun `bank select waits for the next program change`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GM2_ON),
                ControlChangeEvent(10, 2, 0, 120),
                ProgramEvent(50, 2, 0),
            )
        )

        assertEquals(Melody, timeline.stateAt(2, 49))
        assertEquals(Rhythm, timeline.stateAt(2, 50))
    }

    @Test
    fun `messages are read in time order across tracks`() {
        // As if the reset came from one track and the part message from another, listed out of order.
        val events: List<Event> = listOf(
            SysexEvent(100, gsUseForRhythmPart(part = 0x0A, value = 1)),
            SysexEvent(0, GS_RESET),
        )

        assertEquals(Rhythm, ChannelSetupTimeline.from(events).stateAt(10, 100))
    }

    @Test
    fun `bank select is latched at the next program change`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GS_RESET),
                ProgramEvent(0, 0, 4),
                ControlChangeEvent(50, 0, 0, 8),
                ControlChangeEvent(50, 0, 32, 2),
                ProgramEvent(100, 0, 5),
            )
        )

        assertEquals(ChannelSetup(MidiMode.GS, Melody, 0, 0, 4), timeline.setupAt(0, 99), "Before the program change")
        assertEquals(ChannelSetup(MidiMode.GS, Melody, 8, 2, 5), timeline.setupAt(0, 100), "At the program change")
    }

    @Test
    fun `a bank stays selected for later program changes`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GS_RESET),
                ControlChangeEvent(0, 0, 0, 8),
                ProgramEvent(0, 0, 4),
                ProgramEvent(100, 0, 5),
            )
        )

        assertEquals(8, timeline.setupAt(0, 100).msb)
    }

    @Test
    fun `a reset puts the banks back to their defaults but keeps the program`() {
        val timeline = ChannelSetupTimeline.from(
            listOf(
                SysexEvent(0, GS_RESET),
                ControlChangeEvent(0, 0, 0, 8),
                ProgramEvent(0, 0, 4),
                SysexEvent(100, GM2_ON),
            )
        )

        assertEquals(ChannelSetup(MidiMode.GM2, Melody, 121, 0, 4), timeline.setupAt(0, 100))
        assertEquals(120, timeline.setupAt(9, 100).msb, "GM2 channel 10 starts on the rhythm bank")
    }

    @Test
    fun `each reset sets its own mode`() {
        mapOf(GM_ON to MidiMode.GM, GM2_ON to MidiMode.GM2, GS_RESET to MidiMode.GS, XG_ON to MidiMode.XG)
            .forEach { (reset, mode) ->
                val timeline = ChannelSetupTimeline.from(listOf(SysexEvent(10, reset)))
                assertEquals(mode, timeline.setupAt(3, 10).mode, "After ${reset.hex()}")
            }
    }

    @Test
    @Spec("midi.assignment.mode.settings")
    fun `without a reset of its own, a file is in the mode the synthesizer was reset to`() {
        val timeline = ChannelSetupTimeline.from(emptyList(), initialMode = MidiMode.XG)

        assertEquals(MidiMode.XG, timeline.setupAt(0, 0).mode)
        assertEquals(127, timeline.setupAt(9, 0).msb, "XG channel 10 starts on the drum bank")
        assertEquals(MidiMode.GM, ChannelSetupTimeline.from(emptyList()).setupAt(0, 0).mode, "GM when not told otherwise")
    }

    @Test
    @Spec("midi.assignment.mode.reset-message")
    fun `a reset in the file overrides the mode the synthesizer was reset to`() {
        val timeline = ChannelSetupTimeline.from(listOf(SysexEvent(0, GS_RESET)), initialMode = MidiMode.XG)

        assertEquals(MidiMode.GS, timeline.setupAt(0, 0).mode)
    }

    @Test
    fun `there is no program before a channel's first program change`() {
        val timeline = ChannelSetupTimeline.from(listOf(ProgramEvent(100, 0, 40)))

        assertEquals(null, timeline.setupAt(0, 99).program)
        assertEquals(40, timeline.setupAt(0, 100).program)
    }

    @Test
    fun `the last of several program changes at one tick wins`() {
        val timeline = ChannelSetupTimeline.from(listOf(ProgramEvent(0, 0, 1), ProgramEvent(0, 0, 2)))

        assertEquals(2, timeline.setupAt(0, 0).program)
    }

    @Test
    fun `setups are found correctly between many changes`() {
        val events = (0 until 50).map { ProgramEvent(it * 10, 0, it.toByte()) }
        val timeline = ChannelSetupTimeline.from(events)

        (0 until 50).forEach { i ->
            assertEquals(i, timeline.setupAt(0, i * 10).program, "At the change at ${i * 10}")
            assertEquals(i, timeline.setupAt(0, i * 10 + 9).program, "Just before the change after ${i * 10}")
        }
        assertEquals(49, timeline.setupAt(0, Int.MAX_VALUE).program)
    }

    private companion object {
        val GM_ON = bytes(0xF0, 0x7E, 0x7F, 0x09, 0x01, 0xF7)
        val GM2_ON = bytes(0xF0, 0x7E, 0x7F, 0x09, 0x03, 0xF7)
        val GS_RESET = bytes(0xF0, 0x41, 0x10, 0x42, 0x12, 0x40, 0x00, 0x7F, 0x00, 0x41, 0xF7)
        val XG_ON = bytes(0xF0, 0x43, 0x10, 0x4C, 0x00, 0x00, 0x7E, 0x00, 0xF7)

        fun gsUseForRhythmPart(part: Int, value: Int): ByteArray {
            val address = listOf(0x40, 0x10 or part, 0x15)
            val checksum = (128 - (address.sum() + value) % 128) % 128
            return bytes(0xF0, 0x41, 0x10, 0x42, 0x12, *address.toIntArray(), value, checksum, 0xF7)
        }

        fun xgPartMode(part: Int, mode: Int): ByteArray = bytes(0xF0, 0x43, 0x10, 0x4C, 0x08, part, 0x07, mode, 0xF7)

        fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

        fun ByteArray.hex(): String = joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
