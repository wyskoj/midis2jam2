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

import org.wysko.midis2jam2.util.oneOf

private const val CC_DATA_ENTRY_MSB = 6
private const val CC_DATA_ENTRY_LSB = 38

private const val CC_RPN_LSB = 100
private const val CC_RPN_MSB = 101

/**
 * Represents a Registered Parameter Number (RPN) change.
 *
 * @property time The time this event occurs, in MIDI ticks.
 * @property channel The channel this event occurs on.
 * @property registeredParameterNumber The RPN to change.
 * @property dataEntry The new value of the RPN.
 */
data class MidiRegisteredParameterNumberChangeEvent(
    override val time: Long,
    override val channel: Int,
    val registeredParameterNumber: RegisteredParameterNumber,
    val dataEntry: RpnDataEntry
) : MidiChannelEvent(time, channel) {
    companion object {
        /**
         * Traverses a list of [MidiControlChangeEvent] and returns a list of [MidiRegisteredParameterNumberChangeEvent]
         * that have occurred.
         *
         * @param events The list of [MidiControlChangeEvent] to process.
         * @param registeredParameterNumber The RPN to collect.
         * @return A list of [MidiRegisteredParameterNumberChangeEvent] that have occurred.
         */
        fun collectRegisteredParameterNumberChanges(
            events: List<MidiControlChangeEvent>,
            registeredParameterNumber: RegisteredParameterNumber
        ): List<MidiRegisteredParameterNumberChangeEvent> {
            val list = mutableListOf<MidiRegisteredParameterNumberChangeEvent>()

            if (events.isEmpty()) return list

            val controllers = mutableMapOf(
                CC_RPN_LSB to 0x7F,
                CC_RPN_MSB to 0x7F,
                CC_DATA_ENTRY_MSB to 0x00,
                CC_DATA_ENTRY_LSB to 0x00
            )

            return events.fold(list) { acc: MutableList<MidiRegisteredParameterNumberChangeEvent>,
                                       (time, channel, controller, value): MidiControlChangeEvent ->

                controllers[controller] = value

                // RPNs must be set to modify sensitivity.
                if (controllers[CC_RPN_LSB] != registeredParameterNumber.lsb ||
                    controllers[CC_RPN_MSB] != registeredParameterNumber.msb
                ) {
                    return@fold acc
                }

                // RPNs are set. Are we entering data?
                if (controller.oneOf(CC_DATA_ENTRY_MSB, CC_DATA_ENTRY_LSB)) {
                    acc += MidiRegisteredParameterNumberChangeEvent(
                        time,
                        channel,
                        registeredParameterNumber,
                        RpnDataEntry(
                            controllers[CC_DATA_ENTRY_MSB]!!,
                            controllers[CC_DATA_ENTRY_LSB]!!
                        )
                    )
                }
                acc
            }
        }
    }
}
