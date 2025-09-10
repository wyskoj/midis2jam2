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

package org.wysko.midis2jam2.domain

import org.wysko.midis2jam2.midi.system.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.ShortMessage
import javax.sound.midi.SysexMessage

actual class MidiService {
    actual fun getMidiDevices(): List<MidiDevice> =
        MidiSystem.getMidiDeviceInfo().map { DesktopMidiDevice(it) }.filter { it.name != "Real Time Sequencer" }
}

class DesktopMidiDevice(
    val deviceInfo: javax.sound.midi.MidiDevice.Info,
) : MidiDevice {
    override val name: String
        get() = deviceInfo.name

    private lateinit var device: javax.sound.midi.MidiDevice

    override fun open() {
        device = MidiSystem.getMidiDevice(deviceInfo).also { it.open() }
    }

    override fun close() {
        device.close()
    }

    override fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int) {
        device.receiver.send(ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity), -1)
    }

    override fun sendNoteOffMessage(channel: Int, note: Int) {
        device.receiver.send(ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0), -1)
    }

    override fun sendControlChangeMessage(channel: Int, controller: Int, value: Int) {
        device.receiver.send(ShortMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value), -1)
    }

    override fun sendProgramChangeMessage(channel: Int, program: Int) {
        device.receiver.send(ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0), -1)
    }

    override fun sendPitchBendMessage(channel: Int, pitch: Int) {
        val lsb = pitch and 0x7F
        val msb = (pitch shr 7) and 0x7F
        device.receiver.send(ShortMessage(ShortMessage.PITCH_BEND, channel, lsb, msb), -1)
    }

    override fun sendChannelPressureMessage(channel: Int, pressure: Int) {
        device.receiver.send(ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, pressure, 0), -1)
    }

    override fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int) {
        device.receiver.send(ShortMessage(ShortMessage.POLY_PRESSURE, channel, note, pressure), -1)
    }

    override fun sendSysex(data: ByteArray) {
        device.receiver.send(SysexMessage(data, data.size), -1)
    }
}

actual fun MidiDevice.isInternal(): Boolean = name == "Gervill"