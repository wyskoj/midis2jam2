/*
 * Copyright (C) 2025 Jacob Wysko
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

import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Computes the visibility of instruments based on the time of the last hit and the time of the next hit.
 */
@Suppress("DuplicatedCode")
object Visibility {

    /**
     * Determines the visibility of an instrument based on the time of the last hit and the time of the next hit,
     * according to the standard rules.
     *
     * @param T The type of the event collected by the [collector].
     * @param context Context to the main class.
     * @param collector The collector that is operating on these events.
     * @param time The current time.
     * @param parameters The parameters to use.
     * @return Whether the instrument should be visible.
     */
    @Suppress("NestedBlockDepth")
    fun <T : MidiEvent> standardRules(
        context: PerformanceManager,
        collector: EventCollector<T>,
        time: Duration,
        parameters: Parameters = Parameters(),
    ): Boolean = with(context) {
        collector.peek()?.let {
            if (this.time(it) - time <= parameters.showBefore) return true
        }

        collector.peek()?.let { peek ->
            collector.prev()?.let { prev ->
                if (this.time(peek) - this.time(prev) <= parameters.showBetween) return true
            }
        }

        collector.prev()?.let {
            if (time - this.time(it) <= parameters.showAfter) return true
        }

        // Invisible.
        return false
    }

    /**
     * Determines the visibility of an instrument based on the time of the last hit and the time of the next hit,
     * according to the standard rules.
     *
     * @param collector The collector operating on the arcs.
     * @param time The curren time.
     * @param parameters The parameters to use.
     * @return Whether the instrument should be visible.
     */
    @Suppress("ReturnCount")
    fun standardRules(
        collector: TimedArcCollector,
        time: Duration,
        parameters: Parameters = Parameters(),
    ): Boolean {
        if (collector.currentTimedArcs.isNotEmpty()) return true

        collector.peek()?.let {
            if (it.startTime - time <= parameters.showBefore) return true
        }

        collector.prev()?.let { prev ->
            collector.peek()?.let { peek ->
                if (peek.startTime - prev.endTime <= parameters.showBetween) return true
            }
        }

        collector.prev()?.let {
            if (time - it.endTime <= parameters.showAfter) return true
        }

        return false
    }

    private fun PerformanceManager.time(event: Event): Duration = sequence.getTimeOf(event)

    /**
     * Parameters for the visibility algorithm.
     *
     * @property showBefore The amount of time before the last hit that the instrument should be visible.
     * @property showBetween The amount of time between the last hit and the next hit that the instrument should be
     * visible.
     * @property showAfter The amount of time after the last hit that the instrument should be visible.
     */
    data class Parameters(
        val showBefore: Duration = 1.seconds,
        val showBetween: Duration = 7.seconds,
        val showAfter: Duration = 2.seconds,
    )
}
