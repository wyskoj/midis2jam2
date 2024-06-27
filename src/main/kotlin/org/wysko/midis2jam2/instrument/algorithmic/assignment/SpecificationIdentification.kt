/*
 * Copyright (C) 2024 Jacob Wysko
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

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.midis2jam2.instrument.algorithmic.assignment.MidiSpecification.*

/**
 * Searches a MIDI file for a [SysexEvent] that has a specification reset.
 *
 * @return The [MidiSpecification] of the MIDI file.
 */
@Deprecated("Should go to kmidi.")
fun StandardMidiFile.identifySpecification(): MidiSpecification =
    with(tracks.flatMap { it.events }.filterIsInstance<SysexEvent>()) {
        return when {
            any { GeneralStandardMidi.matches(it) } -> GeneralStandardMidi
            any { ExtendedGeneralMidi.matches(it) } -> ExtendedGeneralMidi
            else -> GeneralMidi
        }
    }

/**
 * A MIDI specification.
 */
@Deprecated("Should go to kmidi.")
sealed class MidiSpecification(private val resetMessage: Array<Byte?>? = null) {
    /**
     * Checks if a [SysexEvent] matches this specification.
     *
     * @param sysexEvent The [SysexEvent] to check.
     * @return Whether the [SysexEvent] matches this specification.
     */
    fun matches(sysexEvent: SysexEvent): Boolean {
        val data = sysexEvent.data
        if (data.size != resetMessage?.size) return false

        return data.zip(resetMessage).all { (dataByte, resetByte) ->
            resetByte == null || resetByte == dataByte
        }
    }

    /** GM. */
    data object GeneralMidi : MidiSpecification()

    /** Yamaha XG. */
    data object ExtendedGeneralMidi : MidiSpecification(
        arrayOf(
            0x43.toByte(),
            null,
            0x4C.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x7E.toByte(),
            0x00.toByte(),
            0xF7.toByte(),
        ),
    )

    /** Roland GS. */
    data object GeneralStandardMidi : MidiSpecification(
        arrayOf(
            0x41.toByte(),
            null,
            null,
            0x12.toByte(),
            0x40.toByte(),
            0x00.toByte(),
            0x7F.toByte(),
            0x00.toByte(),
            0x41.toByte(),
            0xF7.toByte(),
        ),
    )
}
