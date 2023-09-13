/*
 * Copyright (C) 2023 Jacob Wysko
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

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiEvent

/**
 * Periodically collects elapsed events from a pool of [MidiEvent].
 *
 * @param events The list of events, sorted.
 * @param context Context to the main class.
 * @param triggerCondition This condition must be true to collect an event from the pool.
 */
class EventCollector<T : MidiEvent>(
    private val events: List<T>,
    private val context: Midis2jam2,
    private val triggerCondition: ((MidiEvent, Double) -> Boolean) = { event, time ->
        context.file.eventInSeconds(event) <= time
    }
) {
    init {
        context.registerEventCollector(this)
    }

    private var currentIndex = 0

    /**
     * Advances the play head and returns a list of [MidiEvents][MidiEvent] that have elapsed since the last invocation
     * of this function or the [seek] function.
     */
    fun advanceCollectAll(time: Double): List<T> {
        val startingIndex = currentIndex

        // Keep advancing the play head while we haven't reached the end of the list, and we aren't looking at an
        // event in the future.
        val atEndOfList = currentIndex >= events.size
        while (!atEndOfList && triggerCondition(events[currentIndex], time)) {
            currentIndex++
        }

        // Return the events we iterated over
        return events.subList(startingIndex, currentIndex)
    }

    /**
     * Advances the play head and returns a list of [MidiEvents][MidiEvent] that have elapsed since the last invocation
     * of this function or the [seek] function.
     *
     * If we collected no events during the time elapsed since the last invocation, the return is `null`.
     */
    fun advanceCollectOne(time: Double): T? {
        // Keep advancing the play head while we haven't reached the end of the list, and we aren't looking at an
        // event in the future.
        var advanced = false
        val atEndOfList = currentIndex >= events.size
        while (!atEndOfList && triggerCondition(events[currentIndex], time)) {
            currentIndex++
            advanced = true
        }

        // Return the last event we iterated over
        return if (advanced) events[currentIndex - 1] else null
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song.
     * Don't use this method for each frame.
     * Rather, use this when making large jumps in time.
     */
    fun seek(time: Double) {
        currentIndex = events.indexOfFirst { context.file.eventInSeconds(it) >= time }
        if (currentIndex == -1) currentIndex = 0
    }

    /**
     * Returns the immediate next event in the future. If there are no more events, the return is `null`.
     */
    fun peek(): T? = if (currentIndex < events.size) events[currentIndex] else null

    /**
     * Returns the last elapsed event. If no events have yet elapsed, the return is `null`.
     */
    fun prev(): T? = if (currentIndex > 0) events[currentIndex - 1] else null
}
