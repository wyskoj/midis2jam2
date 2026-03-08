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

package org.wysko.midis2jam2.midi.system

interface MidiDevice {
    val name: String

    fun open()
    fun close()

    fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int)
    fun sendNoteOffMessage(channel: Int, note: Int)
    fun sendControlChangeMessage(channel: Int, controller: Int, value: Int)
    fun sendProgramChangeMessage(channel: Int, program: Int)
    fun sendPitchBendMessage(channel: Int, pitch: Int)
    fun sendChannelPressureMessage(channel: Int, pressure: Int)
    fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int)
    fun sendData(data: ByteArray)
}