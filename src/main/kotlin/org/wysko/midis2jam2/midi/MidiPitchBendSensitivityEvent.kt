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
 * @property time The time this event occurs, in MIDI ticks.
 * @property channel The channel this event occurs on.
 * @property value The new pitch bend sensitivity, in semitones.
 */
data class MidiPitchBendSensitivityEvent(
    override val time: Long,
    override val channel: Int,
    val value: Double
) : MidiChannelEvent(time, channel) {
    companion object {
        /**
         * Parses a list of [MidiRegisteredParameterNumberChangeEvent] and
         * returns a list of [MidiPitchBendSensitivityEvent].
         * The first event in the list is always a default event with a value of 2.0.
         *
         * @param events The list of [MidiRegisteredParameterNumberChangeEvent] to process.
         * @param defaultChannel The default channel to use if the channel is not specified.
         */
        fun fromRpnChanges(
            events: List<MidiRegisteredParameterNumberChangeEvent>,
            defaultChannel: Int
        ): List<MidiPitchBendSensitivityEvent> {
            val list = mutableListOf(MidiPitchBendSensitivityEvent(0L, defaultChannel, 2.0))
            if (events.isEmpty()) return list

            require(events.all { it.registeredParameterNumber == RegisteredParameterNumber.PitchBendSensitivity }) {
                "The list of events must only contain pitch bend sensitivity events."
            }

            return list + events.map {
                MidiPitchBendSensitivityEvent(
                    it.time, it.channel, it.dataEntry.msb + it.dataEntry.lsb.centsToSemitones()
                )
            }
        }
    }
}

private fun Int.centsToSemitones() = this / 100.0
