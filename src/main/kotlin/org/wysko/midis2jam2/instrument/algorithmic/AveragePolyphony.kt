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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.midi.NotePeriodGroup

object AveragePolyphony {
    fun calculate(group: NotePeriodGroup): Double {
        val events =
            group.notePeriods.flatMap { listOf(Event.NoteOn(it.start), Event.NoteOff(it.end)) }.sortedBy { it.time }

        var polyphony = 0
        var lastTime = events.first().time
        var totalPolyphony = 0.0
        for (event in events) {
            totalPolyphony += polyphony * (event.time - lastTime)
            when (event) {
                is Event.NoteOn -> polyphony++
                is Event.NoteOff -> polyphony--
            }
            lastTime = event.time
        }

        return totalPolyphony / group.duration
    }

    private sealed class Event(open val time: Double) {
        data class NoteOn(override val time: Double) : Event(time)
        data class NoteOff(override val time: Double) : Event(time)
    }
}