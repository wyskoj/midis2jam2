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

package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import org.wysko.kmidi.midi.TimedArc
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.algorithmic.TimedArcCollector
import org.wysko.midis2jam2.util.plusAssign
import kotlin.time.Duration

/**
 * Used with [DivisiveSustainedInstrument], this class animates a single note (i.e., A, A#, B, C, etc.).
 *
 * @param context The context to the main class.
 * @param arcs The list of all arcs that this instrument should play.
 * @see DivisiveSustainedInstrument
 */
abstract class PitchClassAnimator protected constructor(context: PerformanceManager, arcs: List<TimedArc>) {

    /**
     * The collector that manages the note periods.
     */
    protected val collector: TimedArcCollector = TimedArcCollector(context, arcs)

    /**
     * The highest level node.
     */
    val root: Node = Node()

    /**
     * The highest level node.
     */
    val animation: Node = Node()

    /**
     * The geometry node.
     */
    val geometry: Node = Node()

    init {
        root += animation
        animation += geometry
    }

    /**
     * `true` if the animator is playing, `false` otherwise.
     *
     * *This is a convenience property for checking if [TimedArcCollector.currentTimedArcs] is empty.*
     */
    val playing: Boolean
        get() = collector.currentTimedArcs.isNotEmpty()

    /** Call this method every frame to update the twelfth. */
    open fun tick(time: Duration, delta: Duration) {
        collector.advance(time)
    }
}
