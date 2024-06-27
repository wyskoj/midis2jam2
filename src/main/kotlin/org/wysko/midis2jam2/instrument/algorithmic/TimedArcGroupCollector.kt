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
import org.wysko.midis2jam2.midi.TimedArcGroup
import kotlin.time.Duration

/**
 * Collects [TimedArcGroup]s that have overlapping times.
 */
class TimedArcGroupCollector(
    context: Midis2jam2,
    private var timedArcGroups: List<TimedArcGroup>,
    private val releaseCondition: (time: Duration, timedArcGroup: TimedArcGroup) -> Boolean =
        { time, timedArcGroup -> time >= timedArcGroup.endTime },
) : Collector<TimedArcGroup> {
    init {
        context.registerCollector(this)
        timedArcGroups = timedArcGroups.sortedBy { it.startTime }
    }

    /**
     * The current set of [TimedArcGroups][TimedArcGroup].
     */
    var currentTimedArcGroup: TimedArcGroup? = null
        private set
    private var currentIndex = 0

    /** Advances the play head forwards to update the set of current note periods, given the current [time]. */
    fun advance(time: Duration): TimedArcGroup? {
        // Collect TimedArcGroups that need to be added to the heap
        var changed = false
        while (currentIndex < timedArcGroups.size && timedArcGroups[currentIndex].startTime <= time) {
            currentTimedArcGroup = timedArcGroups[currentIndex++]
            changed = true
        }

        // Remove TimedArcGroups that have elapsed and need to be removed from the heap
        if (currentTimedArcGroup != null && releaseCondition(time, currentTimedArcGroup ?: return null)) {
            currentTimedArcGroup = null
        }

        return if (changed) currentTimedArcGroup else null
    }

    /**
     * Moves the play head forward or backward in time to "seek" to a new position in the song. Do not use this method
     * for each frame. Rather, use this when making large jumps in time.
     */
    override fun seek(time: Duration) {
        currentTimedArcGroup = null
        currentIndex = 0
        advance(time)
    }

    /** Returns the next [TimedArcGroup] that has not yet started. */
    override fun peek(): TimedArcGroup? = timedArcGroups.getOrNull(currentIndex)

    /** Returns the last fully elapsed [TimedArcGroup]. */
    override fun prev(): TimedArcGroup? = timedArcGroups.getOrNull(currentIndex - 1)
}
