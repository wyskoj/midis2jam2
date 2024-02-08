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

package org.wysko.midis2jam2.midi

/**
 * A MIDI system exclusive event, or sysex event, is a MIDI message that carries system exclusive data.
 * This data can be used to control various parameters of a synthesizer or effects unit.
 *
 * @param data The data to be sent.
 */
data class MidiSysexEvent(
    override val time: Long,
    val data: ByteArray,
) : MidiEvent(time) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MidiSysexEvent

        if (time != other.time) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time
        result = 31 * result + data.contentHashCode()
        return result.toInt()
    }
}
