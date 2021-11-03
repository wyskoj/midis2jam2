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

/** Indicates which instrument should be playing at a certain time. */
data class MidiProgramEvent
    (
    override val time: Long, override val channel: Int,
    val programNum: Int,
) : MidiChannelSpecificEvent(time, channel) {

    companion object {
        /**
         * Given a list of program events, removes duplicate events. There are two types of duplicate events:
         *
         *  * Events that occur at the same time
         *  * Adjacent events that have the same program value
         *
         * For events at the same time, the last of two events is kept (in the order of the list). So, if a list contained
         *
         *     [time = 0, num = 43], [time = 0, num = 24], [time = 0, num = 69]
         *
         * it would afterwards contain
         *
         *     [time = 0, num = 69]
         *
         *
         * For events that have the same program value, the first of two events is kept (in the order of the list). So,
         * if a list contained
         *
         *     [time = 0, num = 50], [time = 128, num = 50], [time = 3000, num = 50]
         *
         * it would afterwards contain
         *
         *     [time = 0, num = 50]
         *
         * @param programEvents the list of program events
         */
        @JvmStatic
        fun removeDuplicateProgramEvents(programEvents: MutableList<MidiProgramEvent>) {
            /* Remove program events at same time (keep the last one) */
            for (i in programEvents.size - 2 downTo 0) {
                while (i < programEvents.size - 1 && programEvents[i].time == programEvents[i + 1].time) {
                    programEvents.removeAt(i)
                }
            }

            /* Remove program events with same value (keep the first one) */for (i in programEvents.size - 2 downTo 0) {
                while (i != programEvents.size - 1 && programEvents[i].programNum == programEvents[i + 1].programNum) {
                    programEvents.removeAt(i + 1)
                }
            }
        }
    }
}