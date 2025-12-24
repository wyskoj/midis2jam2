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

package org.wysko.midis2jam2.instrument.family.percussion.drumset

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.Companion.filterByNotes
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.family.percussion.PercussionInstrument
import org.wysko.midis2jam2.manager.DrumSetVisibilityManager.Companion.drumSetVisibilityManagerReal
import org.wysko.midis2jam2.midi.RIDE_BELL
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_1
import org.wysko.midis2jam2.midi.RIDE_CYMBAL_2
import org.wysko.midis2jam2.util.isFakeShadows
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.plusAssign
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.assetLoader
import kotlin.time.Duration

/**
 * The drum set.
 *
 * @param context The context to the main class.
 * @param events The events that this drum set is responsible for.
 */
abstract class DrumSet(context: PerformanceManager, events: List<NoteEvent.NoteOn>) :
    PercussionInstrument(context, events.toMutableList()) {

    init {
        if (context.isFakeShadows) {
            geometry += context.assetLoader.fakeShadow("Assets/DrumShadow.obj", "Assets/DrumShadow.png").apply {
                loc = v3(0, 0.01, -80)
            }
        }
    }

    override fun calculateVisibility(time: Duration): Boolean =
        with(context.drumSetVisibilityManagerReal) {
            return (isVisible && currentlyVisibleDrumSet == this@DrumSet).also {
                if (!this@DrumSet.isVisible && it) onEntry()
                if (this@DrumSet.isVisible && !it) onExit()
            }
        }

    override fun adjustForMultipleInstances(delta: Duration): Unit = Unit // The drum set is always in the same place.

    companion object {
        /**
         * Given a set of events for ride cymbals, partitions them based on which cymbal each event is for.
         *
         * @param events The events to partition.
         * @return A pair of lists, the first list is for the first cymbal, the second list is for the second cymbal.
         */
        fun partitionRideCymbals(events: List<NoteEvent.NoteOn>): Pair<List<NoteEvent.NoteOn>, List<NoteEvent.NoteOn>> {
            var currentRide = 1
            val ride1Notes = mutableListOf<NoteEvent.NoteOn>()
            val ride2Notes = mutableListOf<NoteEvent.NoteOn>()
            events.filterByNotes(RIDE_BELL, RIDE_CYMBAL_1, RIDE_CYMBAL_2).forEach {
                when (it.note) {
                    RIDE_CYMBAL_1 -> {
                        ride1Notes.add(it)
                        currentRide = 1
                    }

                    RIDE_CYMBAL_2 -> {
                        ride2Notes.add(it)
                        currentRide = 2
                    }

                    RIDE_BELL -> {
                        if (currentRide == 1) {
                            ride1Notes.add(it)
                        } else {
                            ride2Notes.add(it)
                        }
                    }
                }
            }
            return Pair(ride1Notes, ride2Notes)
        }
    }
}
