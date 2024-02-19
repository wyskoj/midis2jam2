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

import org.wysko.midis2jam2.midi.RegisteredParameterNumber.ModulationDepthRange

private const val LSB_TO_SEMITONES = 100.0 / 128.0 / 100.0 // 1 LSB = 100/128 cents

/**
 * A moment where the modulation depth range is changed.
 *
 * This is not a "real" MIDI event, but represents a specific type of [MidiRegisteredParameterNumberChangeEvent].
 *
 * @property time The time this event occurs in MIDI ticks.
 * @property channel The channel this event occurs on.
 * @property value The new modulation depth range in semitones.
 */
data class MidiModulationDepthRangeEvent(
    override val time: Long,
    override val channel: Int,
    val value: Double
) : MidiChannelEvent(time, channel) {

    companion object {
        /**
         * Parses a list of [MidiRegisteredParameterNumberChangeEvent] and
         * returns a list of [MidiModulationDepthRangeEvent].
         * The first event in the list is always a default event with a value of 2.0.
         *
         * @param events The list of [MidiRegisteredParameterNumberChangeEvent] to process.
         * @param defaultChannel The default channel to use if the channel is not specified.
         */
        fun fromRpnChanges(
            events: List<MidiRegisteredParameterNumberChangeEvent>,
            defaultChannel: Int
        ): List<MidiModulationDepthRangeEvent> {
            val list = mutableListOf(MidiModulationDepthRangeEvent(0L, defaultChannel, 0.5))
            if (events.isEmpty()) return list

            require(events.all { it.registeredParameterNumber == ModulationDepthRange }) {
                "The list of events must only contain modulation depth range events."
            }

            return list + events.map {
                MidiModulationDepthRangeEvent(
                    it.time, it.channel, it.dataEntry.msb + (it.dataEntry.lsb * LSB_TO_SEMITONES)
                )
            }
        }
    }
}
