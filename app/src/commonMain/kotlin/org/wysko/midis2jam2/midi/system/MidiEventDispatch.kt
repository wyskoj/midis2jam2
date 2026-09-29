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

package org.wysko.midis2jam2.midi.system

import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent

/**
 * Sends [event] to this device, if it is an event a device can play (channel voice messages and SysEx).
 * Meta events are ignored.
 */
fun MidiDevice.dispatch(event: Event) {
    when (event) {
        is MidiEvent -> when (event) {
            is NoteEvent.NoteOff -> sendNoteOffMessage(event.channel.toInt(), event.note.toInt())
            is NoteEvent.NoteOn -> sendNoteOnMessage(event.channel.toInt(), event.note.toInt(), event.velocity.toInt())
            is ChannelPressureEvent -> sendChannelPressureMessage(event.channel.toInt(), event.pressure.toInt())
            is ControlChangeEvent -> sendControlChangeMessage(
                event.channel.toInt(),
                event.controller.toInt(),
                event.value.toInt()
            )

            is PitchWheelChangeEvent -> sendPitchBendMessage(event.channel.toInt(), event.value.toInt())
            is PolyphonicKeyPressureEvent -> sendPolyphonicPressureMessage(
                event.channel.toInt(),
                event.note.toInt(),
                event.pressure.toInt()
            )

            is ProgramEvent -> sendProgramChangeMessage(event.channel.toInt(), event.program.toInt())
        }

        is SysexEvent -> sendData(event.data)
        else -> Unit
    }
}
