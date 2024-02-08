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
 * The number of microseconds in a beat when the BPM is 120.
 */
const val ONE_HUNDRED_TWENTY_BPM: Int = 500_000

/**
 * Defines how fast the MIDI file should play.
 *
 * @param time   the time
 * @param number the tempo value, expressed in microseconds per pulse
 */
class MidiTempoEvent(time: Long, val number: Int) : MidiEvent(time) {

    /** Returns this tempo's value as expressed in beats per minute. */
    fun bpm(): Double = 60_000_000.0 / number

    /** Returns this tempo's value as expressed in seconds per beat. */
    fun spb(): Double = 60 / bpm()
}
