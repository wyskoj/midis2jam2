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

import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Vector3f
import com.jme3.scene.Node
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
import kotlin.time.Duration

/**
 * The drum set.
 *
 * @param context The context to the main class.
 * @param events The events that this drum set is responsible for.
 */
abstract class DrumSet(context: PerformanceManager, events: List<NoteEvent.NoteOn>) :
    PercussionInstrument(context, events.toMutableList()) {

    /** True if the smart drum sticks play this kit, so its pieces should be built with ghost sticks. */
    protected val isSmartSticks: Boolean = context.config.settings.instrumentSettings.isSmartDrumSticks

    private var smartSticks: SmartDrumSticks? = null

    /** The smart drum sticks' nodes, left hand first. Empty unless the smart drum sticks are on. */
    internal val smartStickNodes: List<Node> get() = smartSticks?.nodes.orEmpty()

    /** Where the smart drum sticks strike each target, by target id, in [geometry]'s space. */
    internal val smartStickTargets: Map<String, Vector3f> get() = smartSticks?.targetPositions.orEmpty()

    /** The kit's pieces. */
    internal var pieces: List<DrumSetInstrument> = emptyList()
        private set

    /** Adds the smart drum sticks, if they're on, to play [pieces]. Call once the pieces are attached to [geometry]. */
    protected fun addSmartSticks(pieces: List<DrumSetInstrument>) {
        this.pieces = pieces
        if (isSmartSticks) smartSticks = SmartDrumSticks(context, geometry, pieces)
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        smartSticks?.tick(time, delta)
    }

    init {
        if (context.isFakeShadows) {
            geometry += context.model(Models.Shadows.Drum).apply {
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
         * Given a set of events for ride cymbals, partitions them based on which cymbal each event is for. The bell is
         * played on whichever ride was last struck.
         *
         * @param events The events to partition.
         * @param splitBellChords Whether a ride hit struck together with the bell of the same ride goes to the other
         * ride instead, so that two sticks playing them aren't both on one cymbal.
         * @return A pair of lists, the first list is for the first cymbal, the second list is for the second cymbal.
         */
        fun partitionRideCymbals(
            events: List<NoteEvent.NoteOn>,
            splitBellChords: Boolean = false,
        ): Pair<List<NoteEvent.NoteOn>, List<NoteEvent.NoteOn>> {
            var currentRide = 1
            val ride1Notes = mutableListOf<NoteEvent.NoteOn>()
            val ride2Notes = mutableListOf<NoteEvent.NoteOn>()
            events.filterByNotes(RIDE_BELL, RIDE_CYMBAL_1, RIDE_CYMBAL_2).groupBy { it.tick }.values.forEach { chord ->
                val onRide1 = mutableListOf<NoteEvent.NoteOn>()
                val onRide2 = mutableListOf<NoteEvent.NoteOn>()
                chord.forEach {
                    when (it.note) {
                        RIDE_CYMBAL_1 -> currentRide = 1
                        RIDE_CYMBAL_2 -> currentRide = 2
                    }
                    if (currentRide == 1) onRide1 += it else onRide2 += it
                }
                if (splitBellChords) {
                    val toRide2 = onRide1.splitBellChord()
                    val toRide1 = onRide2.splitBellChord()
                    onRide1 += toRide1
                    onRide2 += toRide2
                }
                ride1Notes += onRide1
                ride2Notes += onRide2
            }
            return Pair(ride1Notes, ride2Notes)
        }

        /** If these notes, struck at once on one ride, include its bell and its edge, removes and returns the edge. */
        private fun MutableList<NoteEvent.NoteOn>.splitBellChord(): List<NoteEvent.NoteOn> {
            if (none { it.note == RIDE_BELL } || all { it.note == RIDE_BELL }) return emptyList()
            val edge = filter { it.note != RIDE_BELL }
            removeAll(edge)
            return edge
        }
    }
}
