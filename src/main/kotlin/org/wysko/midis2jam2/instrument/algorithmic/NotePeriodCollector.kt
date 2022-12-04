/*
 * Copyright (C) 2022 Jacob Wysko
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
import org.wysko.midis2jam2.datastructure.HeapAPQ
import org.wysko.midis2jam2.midi.NotePeriod

/** Periodically updates a set of [NotePeriods][NotePeriod] that are "elapsing" at the given time. */
class NotePeriodCollector(
    private var notePeriods: List<NotePeriod>,
    context: Midis2jam2,
    /** Releases notes early by this much. */
    private val releaseCondition: ((time: Double, np: NotePeriod) -> Boolean) = { time: Double, notePeriod: NotePeriod ->
        time >= notePeriod.endTime
    }
) {
    init {
        context.registerNotePeriodCollector(this)
        notePeriods = notePeriods.sortedBy { it.startTime }
    }

    private val currentNotePeriods = HashSet<NotePeriod>()
    private val heap = HeapAPQ<Double, NotePeriod> { p0, p1 -> p0.compareTo(p1) }
    private var currentIndex = 0
    private var lastRemoved: NotePeriod? = null

    /** Advances the play head forwards to update the set of current note periods, given the current [time]. */
    fun advance(time: Double): Set<NotePeriod> {
        // Collect NotePeriods that need to be added to the heap
        while (currentIndex < notePeriods.size && notePeriods[currentIndex].startTime <= time) {
            val np = notePeriods[currentIndex]
            heap.insert(np.endTime, np) // Add to heap, sorting by endTime
            currentNotePeriods.add(np) // Add to set of current NotePeriods
            currentIndex++
        }

        // Remove NotePeriods that have elapsed and need to be removed from the heap
        while (heap.min()?.let { releaseCondition(time, it.value) } == true) {
            heap.removeMin()?.let {
                currentNotePeriods.remove(it.value)
                lastRemoved = it.value
            }
        }

        return currentNotePeriods
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song. Do not use this method
     * for each frame. Rather, use this when making large jumps in time.
     */
    fun seek(time: Double): Set<NotePeriod> {
        // Clear fields
        currentNotePeriods.clear()
        heap.clear()
        currentIndex = 0
        lastRemoved = null

        // Advance forward to desired time
        return advance(time)
    }

    /** Returns the next [NotePeriod] that has not yet started. */
    fun peek(): NotePeriod? = notePeriods.getOrNull(currentIndex)

    /** Returns the last fully elapsed [NotePeriod]. */
    fun prev(): NotePeriod? = lastRemoved
}
