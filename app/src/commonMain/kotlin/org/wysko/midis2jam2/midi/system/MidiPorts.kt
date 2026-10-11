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

import org.wysko.midis2jam2.midi.PortedEvent

/**
 * The [MidiDevice] each MIDI port of a file is played on.
 *
 * A file with MIDI Port events has 16 channels on each port, and each port needs a device of its own to hear them
 * apart. The built-in synthesizers can make as many as a file needs. An external device is only one port, so every
 * port is played on it, as if the file had none.
 *
 * A port beyond the last device shares one with an earlier port: port `n` is played on device `n % size`.
 *
 * @property devices The device for each port, in port order. The same device may appear more than once.
 */
class MidiPorts(val devices: List<MidiDevice>) {
    init {
        require(devices.isNotEmpty()) { "A file needs at least one port to play on." }
    }

    /** Every device, once each, however many ports it plays. */
    val distinctDevices: List<MidiDevice> = devices.distinct()

    /** The number of ports. */
    val size: Int
        get() = devices.size

    /** The device [port] is played on. */
    operator fun get(port: Int): MidiDevice = devices[port % devices.size]

    /** Opens every device. */
    fun open(): Unit = distinctDevices.forEach { it.open() }

    /** Closes every device. */
    fun close(): Unit = distinctDevices.forEach { it.close() }

    /** Sends [event] to the device for its port. */
    fun dispatch(event: PortedEvent): Unit = this[event.port].dispatch(event.event)

    /** Sends [data] to every device, as a system exclusive message. */
    fun sendData(data: ByteArray): Unit = distinctDevices.forEach { it.sendData(data) }

    companion object {
        /** Every port played on [device]. */
        fun of(device: MidiDevice): MidiPorts = MidiPorts(listOf(device))
    }
}
