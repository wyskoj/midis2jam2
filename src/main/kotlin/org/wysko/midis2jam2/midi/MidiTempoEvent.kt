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
 * Defines how fast the MIDI file should play.
 *
 * @property time The time of the event in MIDI ticks.
 * @property number The tempo value, expressed in microseconds per pulse.
 */
data class MidiTempoEvent(override val time: Long, val number: Int) : MidiEvent(time) {
    /**
     * The tempo value, as expressed in beats per minute.
     */
    val bpm: Double = 60_000_000.0 / number

    /**
     * The tempo value, as expressed in seconds per beat.
     */
    val spb: Double = 60 / bpm

    companion object {
        /**
         * The default tempo event, which is 120 BPM at the beginning of the MIDI file.
         */
        val DEFAULT: MidiTempoEvent = MidiTempoEvent(0, 500_000)
    }
}
