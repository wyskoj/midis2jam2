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
 * This is not a real MIDI event, but represents a moment where the pitch bend intensity is changed. Setting CC#100 and
 * CC#101 to 0, then modifying CC#6 modifies the pitch bend sensitivity of the channel by semitones, and CC#38 modifies
 * the pitch bend sensitivity of the channel by cents.
 *
 * @param value the intensity of the pitch bend, represented in semitones
 */
data class MidiPitchBendSensitivityEvent(
    override val time: Long,
    override val channel: Int,
    val value: Float
) : MidiChannelSpecificEvent(time, channel)
