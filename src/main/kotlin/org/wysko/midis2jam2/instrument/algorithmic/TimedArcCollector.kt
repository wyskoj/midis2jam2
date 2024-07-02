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

import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.datastructure.HeapAPQ
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * A collector that manages a set of [TimedArc]s.
 */
class TimedArcCollector(
    context: Midis2jam2,
    private var arcs: List<TimedArc>,
    private val releaseCondition: (time: Duration, np: TimedArc) -> Boolean = { time: Duration, arc: TimedArc ->
        time >= arc.endTime
    },
) : Collector<TimedArc> {

    init {
        context.registerCollector(this)
        arcs = arcs.sortedBy { it.startTime }
    }

    /**
     * The current set of [TimedArc]s.
     */
    val currentTimedArcs: HashSet<TimedArc> = HashSet()
    private val heap = HeapAPQ<Double, TimedArc> { p0, p1 -> p0.compareTo(p1) }
    private var currentIndex = 0
    private var lastRemoved: TimedArc? = null

    /** Advances the play head forwards to update the set of current note periods, given the current [time]. */
    fun advance(time: Duration): AdvanceResult {
        var hasChanged = false
        val newlyRemovedTimedArcs = mutableSetOf<TimedArc>()
        // Collect Arcs that need to be added to the heap
        while (currentIndex < arcs.size && arcs[currentIndex].startTime <= time) {
            val np = arcs[currentIndex]
            heap.insert(np.endTime.toDouble(SECONDS), np)
            newlyRemovedTimedArcs += np
            currentTimedArcs.add(np)
            currentIndex++
            hasChanged = true
        }

        // Remove Arcs that have elapsed and need to be removed from the heap
        while (heap.min()?.let { releaseCondition(time, it.value) } == true) {
            heap.removeMin()?.let {
                currentTimedArcs.remove(it.value)
                lastRemoved = it.value
                hasChanged = true
            }
        }

        return AdvanceResult(currentTimedArcs, newlyRemovedTimedArcs, hasChanged)
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song. Do not use this method
     * for each frame. Rather, use this when making large jumps in time.
     */
    override fun seek(time: Duration) {
        currentTimedArcs.clear()
        heap.clear()
        currentIndex = 0
        lastRemoved = null
        advance(time)
    }

    /** Returns the next [TimedArc] that has not yet started. */
    override fun peek(): TimedArc? = arcs.getOrNull(currentIndex)

    /** Returns the last fully elapsed [TimedArc]. */
    override fun prev(): TimedArc? = lastRemoved

    class AdvanceResult(val currentTimedArcs: Set<TimedArc>, val newlyRemovedTimedArcs: Set<TimedArc>, val hasChanged: Boolean)
}
