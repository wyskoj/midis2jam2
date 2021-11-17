/*
 * Copyright (C) 2021 Jacob Wysko
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

/** Defines how fast the MIDI file should play. */
class MidiTempoEvent
/**
 * Instantiates a new MIDI tempo event.
 *
 * @param time   the time
 * @param number the tempo value, expressed in microseconds per pulse
 */
    (
    time: Long,
    /** The tempo value, expressed in microseconds per pulse. */
    val number: Int,
) : MidiEvent(time) {

    /**
     * Expresses this tempo event in beats per minute.
     *
     * @return the tempo, in beats per minute
     */
    private fun beatsPerMinute(): Double {
        return 6E7 / number
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MidiTempoEvent

        if (number != other.number) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + number
        return result
    }


}