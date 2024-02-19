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

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.NotePeriod
import org.wysko.midis2jam2.midi.NotePeriodGroup

/** Periodically updates a set of [NotePeriods][NotePeriod] that are "elapsing" at the given time. */
class NotePeriodGroupCollector(
    context: Midis2jam2,
    private var notePeriodGroups: List<NotePeriodGroup>,
    private val releaseCondition: (time: Double, notePeriodGroup: NotePeriodGroup) -> Boolean =
        { time: Double, notePeriodGroup: NotePeriodGroup -> time >= notePeriodGroup.endTime },
) : Collector<NotePeriodGroup> {
    init {
        context.registerCollector(this)
        notePeriodGroups = notePeriodGroups.sortedBy { it.startTime }
    }

    /**
     * The current set of [NotePeriods][NotePeriod].
     */
    var currentNotePeriodGroup: NotePeriodGroup? = null
        private set
    private var currentIndex = 0

    /** Advances the play head forwards to update the set of current note periods, given the current [time]. */
    fun advance(time: Double): NotePeriodGroup? {
        // Collect NotePeriods that need to be added to the heap
        var changed = false
        while (currentIndex < notePeriodGroups.size && notePeriodGroups[currentIndex].startTime <= time) {
            currentNotePeriodGroup = notePeriodGroups[currentIndex++]
            changed = true
        }

        // Remove NotePeriods that have elapsed and need to be removed from the heap
        if (currentNotePeriodGroup != null && releaseCondition(time, currentNotePeriodGroup ?: return null)) {
            currentNotePeriodGroup = null
        }

        return if (changed) currentNotePeriodGroup else null
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song. Do not use this method
     * for each frame. Rather, use this when making large jumps in time.
     */
    override fun seek(time: Double) {
        currentNotePeriodGroup = null
        currentIndex = 0
        advance(time)
    }

    /** Returns the next [NotePeriod] that has not yet started. */
    override fun peek(): NotePeriodGroup? = notePeriodGroups.getOrNull(currentIndex)

    /** Returns the last fully elapsed [NotePeriod]. */
    override fun prev(): NotePeriodGroup? = notePeriodGroups.getOrNull(currentIndex - 1)
}
