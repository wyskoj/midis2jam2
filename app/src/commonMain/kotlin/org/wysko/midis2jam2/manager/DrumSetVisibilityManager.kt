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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.Visibility
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSet
import org.wysko.midis2jam2.manager.PlaybackManager.Companion.time

class DrumSetVisibilityManager() : BaseManager() {
    private lateinit var drumSets: List<DrumSet>
    private lateinit var collectors: Map<DrumSet, EventCollector<NoteEvent.NoteOn>>

    var currentlyVisibleDrumSet: DrumSet? = null
        private set

    var isVisible: Boolean = false

    override fun initialize(app: Application) {
        super.initialize(app)
        drumSets = context.instruments.filterIsInstance<DrumSet>()
        collectors = drumSets.associateWith { it.collectorForVisibility }
        currentlyVisibleDrumSet = when {
            drumSets.isEmpty() -> null
            else -> collectors.entries.minBy { it.value.peek()?.tick ?: Int.MAX_VALUE }.key
        }
    }

    override fun update(tpf: Float) {
        val collectorResults = collectors.mapValues { it.value.advanceCollectOne(app.time) }

        val individuallyComputedVisibilities = collectors.entries.associateWith {
            Visibility.standardRules(
                context = context,
                collector = it.value,
                time = app.time,
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

    companion object {
        val PerformanceManager.drumSetVisibilityManagerReal: DrumSetVisibilityManager
            get() = this.app.stateManager.getState(DrumSetVisibilityManager::class.java)
    }
}