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

import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Computes the visibility of instruments based on the time of the last hit and the time of the next hit.
 */
@Suppress("DuplicatedCode")
object Visibility {

    context(Midis2jam2)
    private val MidiEvent.sec: Duration
        get() = sequence.getTimeOf(this)

    /**
     * Determines the visibility of an instrument based on the time of the last hit and the time of the next hit,
     * according to the standard rules.
     *
     * @param T The type of the event collected by the [collector].
     * @param context The context to use.
     * @param collector The collector to use.
     * @param time The time to use.
     * @param parameters The parameters to use.
     * @return Whether the instrument should be visible.
     */
    @Suppress("NestedBlockDepth")
    fun <T : MidiEvent> standardRules(
        context: Midis2jam2,
        collector: EventCollector<T>,
        time: Duration,
        parameters: Parameters = Parameters(),
    ): Boolean = with(context) {
        collector.peek()?.let {
            if (it.sec - time <= parameters.showBefore) return true
        }

        collector.peek()?.let { peek ->
            collector.prev()?.let { prev ->
                if (peek.sec - prev.sec <= parameters.showBetween) return true
            }
        }

        collector.prev()?.let {
            if (time - it.sec <= parameters.showAfter) return true
        }

        // Invisible.
        return false
    }

    /**
     * Determines the visibility of an instrument based on the time of the last hit and the time of the next hit,
     * according to the standard rules.
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
