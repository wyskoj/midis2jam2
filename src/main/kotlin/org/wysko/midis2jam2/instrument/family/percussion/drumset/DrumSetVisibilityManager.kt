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

package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.Visibility
import kotlin.time.Duration

/**
 * Manages the visibility of multiple drum sets.
 *
 * @param context The context to the main class.
 * @param drumSets The drum sets to manage the visibility of.
 */
class DrumSetVisibilityManager(private val context: Midis2jam2, drumSets: List<DrumSet>) {

    private val collectors = drumSets.associateWith { it.collectorForVisibility }

    /**
     * The drum set that is currently visible.
     */
    var currentlyVisibleDrumSet: DrumSet? =
        if (drumSets.isEmpty()) null else collectors.entries.minBy { it.value.peek()?.tick ?: Int.MAX_VALUE }.key

    /**
     * `true` if the drum set is visible, `false` otherwise.
     */
    var isVisible: Boolean = false

    /**
     * Updates this manager's state.
     *
     * @param time The current time.
     */
    fun tick(time: Duration) {
        val collectorResults = collectors.mapValues { it.value.advanceCollectOne(time) }

        val individuallyComputedVisibilities = collectors.entries.associateWith {
            Visibility.standardRules(
                context = context,
                collector = it.value,
                time = time,
            )
        }
        // If we go from a non-visible state to a visible state, we want to make sure that the drum set that is
        // currently visible is the one that is going to play next.
        if (!isVisible && individuallyComputedVisibilities.values.any { it }) {
            currentlyVisibleDrumSet = collectors.minBy { it.value.peek()?.tick ?: Int.MAX_VALUE }.key
        }

        isVisible = individuallyComputedVisibilities.values.any { it }
        collectorResults.filter { it.value != null }.forEach {
            currentlyVisibleDrumSet = it.key
        }
    }
}
