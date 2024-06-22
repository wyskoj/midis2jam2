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

import org.wysko.midis2jam2.Midis2jam2

/**
 * A pitch bend event whose absolute semitone value is known.
 *
 * MIDI pitch bend events are dependent on the current pitch bend range, which is separate from the pitch bend event
 * itself. This class is used to represent pitch bend events where the absolute semitone value has been calculated.
 *
 * @property time The time this event occurs in MIDI ticks.
 * @property channel The channel on which the pitch-bend change should occur.
 * @property value The new value of the pitch-bend, expressed in semitones.
 */
class MidiAbsolutePitchBendEvent(
    override val time: Long,
    override val channel: Int,
    val value: Double
) : MidiChannelEvent(time, channel) {
    companion object {
        private const val PITCH_BEND_CENTER = 0x2000

        /**
         * Converts a list of [MidiChannelEvent] into a list of [MidiAbsolutePitchBendEvent].
         *
         * MIDI pitch bend events are dependent on the current pitch bend range, which is separate from the pitch bend event
         * itself. This method calculates the absolute semitone value for each pitch bend event based on the pitch bend range.
         *
         * @param events The list of [MidiChannelEvent] to convert.
         * @param context If you want to have the new events registered in the context, pass it here.
         * @return A list of [MidiAbsolutePitchBendEvent] representing the absolute semitone value of each pitch bend event.
         */
        fun fromEvents(events: List<MidiChannelEvent>, context: Midis2jam2? = null): List<MidiAbsolutePitchBendEvent> {
            var pitchBendRange = 2.0

            val rpnChanges = MidiRegisteredParameterNumberChangeEvent.collectRegisteredParameterNumberChanges(
                events.filterIsInstance<MidiControlChangeEvent>(),
                RegisteredParameterNumber.PitchBendSensitivity
            )
            val sensitivityEvents = MidiPitchBendSensitivityEvent.fromRpnChanges(rpnChanges, events.first().channel)
            val bendEvents = events.filterIsInstance<MidiPitchBendEvent>()

            val inputEvents = (sensitivityEvents + bendEvents).sortedBy { it.time }
            val outputEvents = mutableListOf<MidiAbsolutePitchBendEvent>()

            for (event in inputEvents) {
                when (event) {
                    is MidiPitchBendEvent -> {
                        outputEvents.add(
                            MidiAbsolutePitchBendEvent(
                                event.time, event.channel,
                                (event.value - PITCH_BEND_CENTER) * pitchBendRange / 0x2000
                            )
                        )
                    }

                    is MidiPitchBendSensitivityEvent -> {
                        pitchBendRange = event.value
                    }
                }
            }

            context?.file?.registerEvents(outputEvents)

            return outputEvents
        }
    }
}